#! /usr/bin/env bash
# should be run under user (not root)

mkdir -p data/elasticsearch
chmod g+rwx data/elasticsearch
chgrp 1000 data/elasticsearch