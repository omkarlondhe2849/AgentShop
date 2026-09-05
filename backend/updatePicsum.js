const fs = require('fs');
const file = 'src/main/resources/data.sql';
let data = fs.readFileSync(file, 'utf8');

let seedCounter = 1;
const lines = data.split('\n');
const newLines = lines.map(line => {
  if (line.includes('INSERT INTO product') || !line.trim().startsWith('(')) return line;
  
  // Replace the image url with picsum
  const match = line.match(/'https:\/\/[^']+'/);
  if (match) {
    const newImg = `'https://picsum.photos/seed/AgentShopProduct${seedCounter}/400/400'`;
    seedCounter++;
    return line.replace(/'https:\/\/[^']+'/, newImg);
  }
  return line;
});

fs.writeFileSync(file, newLines.join('\n'));
console.log('Updated data.sql with picsum seed images');
