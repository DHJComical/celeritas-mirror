#!/bin/bash
git grep -orE '\/[*\/^]\?' src/ | awk -F: '{print $1}' | sort | uniq -c | sort -nr
