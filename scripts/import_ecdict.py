#!/usr/bin/env python3
"""
ECDICT Data Import Script
Converts stardict.csv to a pre-filled SQLite database (ecdict.db) for Android offline dictionary.

This script imports ECDICT dictionary data into SQLite format compatible with the
OfflineDict Android app's Room database schema.

Usage:
    python import_ecdict.py [options]

Options:
    --input FILE      Input CSV file path (default: stardict.csv)
    --output FILE     Output database file path (default: ecdict.db)
    --batch-size N    Batch insert size (default: 10000)
    --no-fts          Skip FTS5 index creation
    --verbose         Show detailed progress

ECDICT CSV format (15 columns):
    word, sw, phonetic, phonetic_us, definition, translation, pos,
    collins, oxford, tag, bnc, frq, exchange, detail, audio

Source: https://github.com/skywind3000/ECDICT
License: CC-BY-SA 4.0
"""

import argparse
import csv
import os
import sqlite3
import sys
import time
from pathlib import Path
from typing import Optional


# Default configuration
DEFAULT_CSV_FILE = "stardict.csv"
DEFAULT_DB_FILE = "ecdict.db"
DEFAULT_BATCH_SIZE = 10000

# Database schema constants
TABLE_NAME = "ecdict"
FTS_TABLE_NAME = "ecdict_fts"

# Column definitions matching Room entity (DictEntry.kt)
COLUMNS = [
    "word",           # TEXT PRIMARY KEY
    "sw",             # TEXT NOT NULL DEFAULT ''
    "phonetic",       # TEXT NOT NULL DEFAULT ''
    "phonetic_us",    # TEXT NOT NULL DEFAULT ''
    "definition",     # TEXT NOT NULL DEFAULT ''
    "translation",    # TEXT NOT NULL DEFAULT ''
    "pos",            # TEXT NOT NULL DEFAULT ''
    "collins",        # INTEGER NOT NULL DEFAULT 0
    "oxford",         # INTEGER NOT NULL DEFAULT 0
    "tag",            # TEXT NOT NULL DEFAULT ''
    "bnc",            # INTEGER NOT NULL DEFAULT 0
    "frq",            # INTEGER NOT NULL DEFAULT 0
    "exchange",       # TEXT NOT NULL DEFAULT ''
    "detail",         # TEXT NOT NULL DEFAULT ''
    "audio",          # TEXT NOT NULL DEFAULT ''
]

# Integer columns for type conversion
INT_COLUMNS = {"collins", "oxford", "bnc", "frq"}


def create_database(db_path: str, verbose: bool = False) -> sqlite3.Connection:
    """
    Create SQLite database with the main table and FTS5 virtual table.

    Args:
        db_path: Path to the database file
        verbose: Enable verbose output

    Returns:
        SQLite connection object
    """
    if verbose:
        print("Creating database structure...")

    conn = sqlite3.connect(db_path)
    cursor = conn.cursor()

    # Enable WAL mode for better performance
    cursor.execute("PRAGMA journal_mode=WAL")

    # Create main table matching DictEntry entity
    cursor.execute(f"""
        CREATE TABLE IF NOT EXISTS {TABLE_NAME} (
            word        TEXT PRIMARY KEY,
            sw          TEXT NOT NULL DEFAULT '',
            phonetic    TEXT NOT NULL DEFAULT '',
            phonetic_us TEXT NOT NULL DEFAULT '',
            definition  TEXT NOT NULL DEFAULT '',
            translation TEXT NOT NULL DEFAULT '',
            pos         TEXT NOT NULL DEFAULT '',
            collins     INTEGER NOT NULL DEFAULT 0,
            oxford      INTEGER NOT NULL DEFAULT 0,
            tag         TEXT NOT NULL DEFAULT '',
            bnc         INTEGER NOT NULL DEFAULT 0,
            frq         INTEGER NOT NULL DEFAULT 0,
            exchange    TEXT NOT NULL DEFAULT '',
            detail      TEXT NOT NULL DEFAULT '',
            audio       TEXT NOT NULL DEFAULT ''
        )
    """)

    # Create index on 'sw' column for efficient sorting
    cursor.execute(f"CREATE INDEX IF NOT EXISTS idx_sw ON {TABLE_NAME}(sw)")

    conn.commit()

    if verbose:
        print(f"  Created table: {TABLE_NAME}")
        print(f"  Created index: idx_sw")

    return conn


def create_fts_table(conn: sqlite3.Connection, verbose: bool = False) -> None:
    """
    Create FTS5 virtual table for full-text search.

    The FTS5 table is configured as a contentless external content table,
    which means it stores its own copy of the indexed content.

    Args:
        conn: SQLite connection
        verbose: Enable verbose output
    """
    cursor = conn.cursor()

    if verbose:
        print("Creating FTS5 virtual table...")

    # Create FTS5 virtual table
    # Using content='' creates a standalone FTS table (not external content)
    # This is simpler and matches how Room handles @Fts5 entities
    cursor.execute(f"""
        CREATE VIRTUAL TABLE IF NOT EXISTS {FTS_TABLE_NAME}
        USING fts5(
            word,
            definition,
            translation,
            tokenize='unicode61'
        )
    """)

    conn.commit()

    if verbose:
        print(f"  Created FTS5 table: {FTS_TABLE_NAME}")


def parse_csv_row(row: list) -> tuple:
    """
    Parse a CSV row and convert to database tuple.

    Args:
        row: List of string values from CSV

    Returns:
        Tuple of values with correct types
    """
    # Ensure we have exactly 15 columns
    while len(row) < 15:
        row.append('')

    # Trim to 15 columns if more
    row = row[:15]

    result = []
    for i, col_name in enumerate(COLUMNS):
        value = row[i].strip() if row[i] else ''

        # Convert integer columns
        if col_name in INT_COLUMNS:
            try:
                value = int(value) if value else 0
            except ValueError:
                value = 0

        result.append(value)

    return tuple(result)


def import_csv(
    conn: sqlite3.Connection,
    csv_path: str,
    batch_size: int = DEFAULT_BATCH_SIZE,
    verbose: bool = False
) -> int:
    """
    Import CSV data into the database in batches.

    Args:
        conn: SQLite connection
        csv_path: Path to the CSV file
        batch_size: Number of rows per batch
        verbose: Enable verbose output

    Returns:
        Total number of records imported
    """
    cursor = conn.cursor()
    batch = []
    total_count = 0
    skipped_count = 0
    start_time = time.time()

    # Prepare INSERT statement
    placeholders = ','.join(['?' for _ in COLUMNS])
    insert_sql = f"INSERT OR IGNORE INTO {TABLE_NAME} VALUES ({placeholders})"

    if verbose:
        print(f"Reading CSV file: {csv_path}")

    try:
        # Try different encodings
        encodings = ['utf-8', 'utf-8-sig', 'latin-1']
        file_handle = None

        for encoding in encodings:
            try:
                file_handle = open(csv_path, 'r', encoding=encoding)
                # Test read first line
                file_handle.readline()
                file_handle.seek(0)
                if verbose:
                    print(f"  Using encoding: {encoding}")
                break
            except UnicodeDecodeError:
                if file_handle:
                    file_handle.close()
                continue

        if file_handle is None:
            raise ValueError(f"Could not determine file encoding for {csv_path}")

        with file_handle as f:
            reader = csv.reader(f)

            for row_num, row in enumerate(reader, 1):
                try:
                    parsed_row = parse_csv_row(row)

                    # Skip empty words
                    if not parsed_row[0]:
                        skipped_count += 1
                        continue

                    batch.append(parsed_row)

                    # Batch insert
                    if len(batch) >= batch_size:
                        cursor.executemany(insert_sql, batch)
                        conn.commit()
                        total_count += len(batch)

                        if verbose or row_num % 100000 == 0:
                            elapsed = time.time() - start_time
                            rate = total_count / elapsed if elapsed > 0 else 0
                            print(f"  Imported {total_count:,} records ({rate:.0f} records/sec)")

                        batch.clear()

                except Exception as e:
                    if verbose:
                        print(f"  Warning: Skipping row {row_num}: {e}")
                    skipped_count += 1
                    continue

        # Insert remaining records
        if batch:
            cursor.executemany(insert_sql, batch)
            conn.commit()
            total_count += len(batch)

    except FileNotFoundError:
        print(f"Error: CSV file not found: {csv_path}")
        sys.exit(1)
    except Exception as e:
        print(f"Error reading CSV: {e}")
        sys.exit(1)

    elapsed = time.time() - start_time
    print(f"Main table import complete: {total_count:,} records in {elapsed:.1f}s")

    if skipped_count > 0:
        print(f"  Skipped {skipped_count:,} invalid records")

    return total_count


def build_fts_index(conn: sqlite3.Connection, verbose: bool = False) -> None:
    """
    Populate FTS5 table with data from the main table.

    Args:
        conn: SQLite connection
        verbose: Enable verbose output
    """
    cursor = conn.cursor()
    print("Building FTS5 index...")
    start_time = time.time()

    if verbose:
        print("  Populating FTS5 table...")

    # Insert data into FTS5 table
    cursor.execute(f"""
        INSERT INTO {FTS_TABLE_NAME}(word, definition, translation)
        SELECT word, definition, translation FROM {TABLE_NAME}
    """)
    conn.commit()

    # Optimize FTS5 index
    cursor.execute(f"INSERT INTO {FTS_TABLE_NAME}({FTS_TABLE_NAME}) VALUES('optimize')")
    conn.commit()

    elapsed = time.time() - start_time
    print(f"FTS5 index build complete: {elapsed:.1f}s")


def optimize_database(conn: sqlite3.Connection, db_path: str, verbose: bool = False) -> None:
    """
    Optimize database for better query performance.

    Args:
        conn: SQLite connection
        db_path: Path to the database file
        verbose: Enable verbose output
    """
    cursor = conn.cursor()
    print("Optimizing database...")

    # Analyze tables for query planner
    cursor.execute("ANALYZE")
    conn.commit()

    # Vacuum to compact database (optional, can be slow)
    if verbose:
        print("  Running VACUUM...")

    # Close connection for VACUUM
    conn.close()

    # Reconnect and VACUUM
    conn = sqlite3.connect(db_path)
    cursor = conn.cursor()
    cursor.execute("VACUUM")
    conn.commit()

    # Get final database size
    db_size = os.path.getsize(db_path)
    print(f"Database size: {db_size / 1024 / 1024:.1f} MB")


def print_summary(db_path: str, total_count: int, start_time: float) -> None:
    """Print import summary."""
    elapsed = time.time() - start_time
    db_size = os.path.getsize(db_path)

    print("\n" + "=" * 50)
    print("Import Complete!")
    print("=" * 50)
    print(f"Output file:     {db_path}")
    print(f"Total records:   {total_count:,}")
    print(f"Database size:   {db_size / 1024 / 1024:.1f} MB")
    print(f"Total time:      {elapsed:.1f} seconds")
    print(f"Import rate:     {total_count / elapsed:.0f} records/sec")
    print("=" * 50)


def main():
    """Main entry point."""
    parser = argparse.ArgumentParser(
        description="Import ECDICT CSV data into SQLite database for OfflineDict app",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
    python import_ecdict.py
    python import_ecdict.py --input mydict.csv --output custom.db
    python import_ecdict.py --batch-size 5000 --verbose

Output:
    The script generates a SQLite database with:
    - ecdict: Main dictionary table (15 columns)
    - ecdict_fts: FTS5 full-text search virtual table
        """
    )

    parser.add_argument(
        '--input', '-i',
        default=DEFAULT_CSV_FILE,
        help=f'Input CSV file path (default: {DEFAULT_CSV_FILE})'
    )
    parser.add_argument(
        '--output', '-o',
        default=DEFAULT_DB_FILE,
        help=f'Output database file path (default: {DEFAULT_DB_FILE})'
    )
    parser.add_argument(
        '--batch-size', '-b',
        type=int,
        default=DEFAULT_BATCH_SIZE,
        help=f'Batch insert size (default: {DEFAULT_BATCH_SIZE})'
    )
    parser.add_argument(
        '--no-fts',
        action='store_true',
        help='Skip FTS5 index creation'
    )
    parser.add_argument(
        '--verbose', '-v',
        action='store_true',
        help='Show detailed progress'
    )

    args = parser.parse_args()

    # Print banner
    print("\n" + "=" * 50)
    print("ECDICT Import Tool for OfflineDict")
    print("=" * 50)
    print(f"Input:  {args.input}")
    print(f"Output: {args.output}")
    print(f"Batch:  {args.batch_size}")
    print(f"FTS5:   {'disabled' if args.no_fts else 'enabled'}")
    print("=" * 50 + "\n")

    # Validate input file
    if not os.path.exists(args.input):
        print(f"Error: Input file not found: {args.input}")
        print("\nPlease download ECDICT data first:")
        print("  git clone https://github.com/skywind3000/ECDICT.git")
        print("  or download stardict.csv from the releases")
        sys.exit(1)

    total_start = time.time()

    # Remove existing database
    if os.path.exists(args.output):
        if args.verbose:
            print(f"Removing existing database: {args.output}")
        os.remove(args.output)

    # Step 1: Create database structure
    conn = create_database(args.output, args.verbose)

    # Step 2: Import CSV data
    print("\nStep 1: Importing CSV data...")
    total_count = import_csv(conn, args.input, args.batch_size, args.verbose)

    # Step 3: Build FTS5 index
    if not args.no_fts:
        print("\nStep 2: Building FTS5 index...")
        create_fts_table(conn, args.verbose)
        build_fts_index(conn, args.verbose)
    else:
        print("\nStep 2: Skipping FTS5 index (--no-fts specified)")

    # Step 4: Optimize database
    print("\nStep 3: Optimizing database...")
    optimize_database(conn, args.output, args.verbose)

    # Print summary
    print_summary(args.output, total_count, total_start)

    # Print next steps
    print("\nNext steps:")
    assets_path = Path("app/src/main/assets/ecdict.db")
    print(f"  1. Copy the database to your Android project:")
    print(f"     cp {args.output} <project_path>/{assets_path}")
    print(f"  2. Room will automatically use this pre-filled database")


if __name__ == "__main__":
    main()