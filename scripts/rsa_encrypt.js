const fs = require('fs')
const JSEncrypt = require('../jbm-admin-vue/node_modules/jsencrypt')

const plain = process.argv[2]
const keyFile = process.argv[3]
if (!plain || !keyFile) {
  console.error('usage: node rsa_encrypt.js <plain> <pubkey-b64-file>')
  process.exit(1)
}
const b64 = fs.readFileSync(keyFile, 'utf8').replace(/\s/g, '')
const lines = b64.match(/.{1,64}/g) || [b64]
const pem = `-----BEGIN PUBLIC KEY-----\n${lines.join('\n')}\n-----END PUBLIC KEY-----`
const enc = new JSEncrypt()
enc.setPublicKey(pem)
const out = enc.encrypt(plain)
if (!out) process.exit(2)
process.stdout.write(out)
