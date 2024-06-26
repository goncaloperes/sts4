#!/bin/bash
set -e
workdir=`pwd`

vsix_files=`ls ${workdir}/vsix_folder/vscode-*.vsix`

for vsix_file in $vsix_files
do
    echo "****************************************************************"
    echo "*** Pre-Release Publishing : ${vsix_file}"
    echo "****************************************************************"
    echo ""
    echo "We are runing the following command:"
    echo ""
    echo "     vsce publish --pre-release -p vsce_token --packagePath $vsix_file"
    echo ""
    vsce publish --pre-release -p $vsce_token --packagePath $vsix_file
done
