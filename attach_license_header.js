/*
 * Copyright (c) 2020-2024 IBA Group.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   IBA Group
 *   Zowe Community
 */

const path = require('path')
const fs = require('fs')

const licenseHeader =
`/*
 * Copyright (c) 2020-2024 IBA Group.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   IBA Group
 *   Zowe Community
 */

`

const srcPath = path.resolve(__dirname, 'src', 'main')
const testPath = path.resolve(__dirname, 'src', 'test')
let sources = [];

const walkThroughDir = (dir) => {
  fs
    .readdirSync(dir)
    .forEach((source) => {
      const absPath = path.join(dir, source)
      if (fs.statSync(absPath).isDirectory()) {
        return walkThroughDir(absPath)
      } else {
        if (!absPath.includes('resources')) {
          return sources.push(absPath)
        }
      }
    })
}

walkThroughDir(srcPath)
walkThroughDir(testPath)

sources.forEach((source) => {
  fs.readFile(source, 'utf8', (err, fileData) => {
    if (err) {
      console.error(err)
      return
    }
    const lines = fileData.split('\n')
    if (!lines[1] || !lines[1].includes('* This program and the accompanying materials are made available under the terms of the')) {
      const newFileData = `${licenseHeader}${fileData}`
      fs.writeFile(source, newFileData, (err) => {
        if (err) console.log(err)
      })
    }
  })
})
