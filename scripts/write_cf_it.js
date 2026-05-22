const fs = require("fs");
const content = fs.readFileSync("d:/workspaces/JBM7/scripts/cf_it_body.txt", "utf8");
fs.writeFileSync(process.argv[2], content, "utf8");
console.log("ok", process.argv[2], content.length);