#!/bin/bash
git grep -orF '/^' src/ | awk -F: '{print $1}' | sort | uniq -c | sort -nr
