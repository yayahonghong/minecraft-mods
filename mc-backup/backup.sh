#!/bin/bash
set -e
cd "$(dirname "$0")"
git pull
python3 run.py
