const fs = require('fs');
const path = require('path');

const dataSqlPath = path.join(__dirname, 'src', 'main', 'resources', 'data.sql');

if (!fs.existsSync(dataSqlPath)) {
  console.error("data.sql not found at", dataSqlPath);
  process.exit(1);
}

let content = fs.readFileSync(dataSqlPath, 'utf8');

const imageMap = {
  'Laptops': 'https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=800&q=80',
  'Smartphones': 'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=800&q=80',
  'Headphones': 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=800&q=80',
  'Tablets': 'https://images.unsplash.com/photo-1544244015-0df4b3ffc6b0?w=800&q=80',
  'Cameras': 'https://images.unsplash.com/photo-1516035069371-29a1b244cc32?w=800&q=80',
  'Smartwatches': 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=800&q=80'
};

const lines = content.split('\n');
for (let i = 0; i < lines.length; i++) {
  let line = lines[i];
  if (line.startsWith("('")) {
    for (const [category, url] of Object.entries(imageMap)) {
      if (line.includes(`'${category}'`)) {
        line = line.replace(/'https:\/\/picsum\.photos\/[^']+'/, `'${url}'`);
        lines[i] = line;
        break;
      }
    }
  }
}

fs.writeFileSync(dataSqlPath, lines.join('\n'));
console.log("data.sql updated with explicit category images.");
