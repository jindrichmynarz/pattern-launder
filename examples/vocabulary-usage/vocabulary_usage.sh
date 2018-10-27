#!/usr/bin/env bash

set -e

die () {
  echo >&2 "$@"
  exit 1
}

[ -z ${1} ] && die "Please provide a vocabulary prefix as argument to this script."
PREFIX=${1}

# Test if Apache Jena is installed.
command -v arq >/dev/null 2>&1 ||
die "Please install Apache Jena's arq!"

TMPFILE=$(mktemp)
curl --silent --fail --output ${TMPFILE} \
  http://prefix.cc/${PREFIX}.file.txt ||
die "Namespace prefix ${PREFIX} not found."

VOCABULARY=$(cut -f2 ${TMPFILE})

# Test if the vocabulary is dereferenceable
curl --head --fail --output /dev/null \
  ${VOCABULARY} 2> /dev/null ||
die "Vocabulary ${VOCABULARY} is not dereferenceable."

arq \
  --data ${VOCABULARY} \
  --query vocabulary_to_patterns.rq \
  --results CSV
