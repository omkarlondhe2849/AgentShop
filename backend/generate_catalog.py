import json
import random

categories = {
    "Smartphones": {
        "brands": ["Apple", "Samsung", "Google", "OnePlus", "Xiaomi", "Motorola", "Nothing"],
        "models": ["Pro Max", "Ultra", "Plus", "Fold", "Flip", "Lite", "FE", "Pro"],
        "prefixes": ["iPhone 16", "iPhone 15", "Galaxy S24", "Galaxy S23", "Pixel 8", "Pixel 9", "OnePlus 12", "Phone (2)"],
        "specs": ['"storage":"256GB"', '"storage":"512GB"', '"ram":"8GB"', '"ram":"12GB"'],
        "base_price": 50000,
        "images": [
            "https://images.unsplash.com/photo-1592899677977-9c10ca588bbd?w=400",
            "https://images.unsplash.com/photo-1605236453806-6ff36852873f?w=400",
            "https://images.unsplash.com/photo-1610945415295-d9bbf067e59c?w=400"
        ]
    },
    "Laptops": {
        "brands": ["Apple", "Dell", "HP", "Lenovo", "Asus", "Acer", "MSI"],
        "models": ["Pro", "Air", "XPS", "Spectre", "ThinkPad", "ROG", "ZenBook", "Legion", "Predator", "LOQ"],
        "prefixes": ["MacBook", "Dell XPS 15", "HP Spectre x360", "Lenovo ThinkPad X1", "Asus ROG Zephyrus", "Acer Predator Helios"],
        "specs": ['"ram":"16GB"', '"ram":"32GB"', '"storage":"1TB SSD"', '"processor":"Core i7"', '"processor":"Core i9"'],
        "base_price": 70000,
        "images": [
            "https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=400",
            "https://images.unsplash.com/photo-1593642632823-8f785ba67e45?w=400",
            "https://images.unsplash.com/photo-1496181133206-80ce9b88a853?w=400"
        ]
    },
    "Headphones": {
        "brands": ["Sony", "Bose", "Sennheiser", "Apple", "Beats", "Jabra", "Skullcandy", "boAt"],
        "models": ["Wireless", "Noise Cancelling", "Earbuds", "Pro", "Max", "Elite", "Sport"],
        "prefixes": ["WH-1000XM5", "QuietComfort", "AirPods", "Momentum", "Studio3", "Elite 85t", "Airdopes"],
        "specs": ['"type":"Over-ear"', '"type":"In-ear"', '"battery":"30h"', '"anc":"Yes"'],
        "base_price": 2000,
        "images": [
            "https://images.unsplash.com/photo-1618366712010-f4ae9c647dcb?w=400",
            "https://images.unsplash.com/photo-1590658268037-6bf12f032f55?w=400",
            "https://images.unsplash.com/photo-1546435770-a3e426bf472b?w=400"
        ]
    },
    "Smartwatches": {
        "brands": ["Apple", "Samsung", "Garmin", "Fitbit", "Amazfit", "Fossil"],
        "models": ["Series 9", "Ultra 2", "Watch 6", "Fenix 7", "Versa 4", "GTR 4"],
        "prefixes": ["Watch", "Galaxy Watch", "Fenix", "Versa", "GTR", "Gen 6"],
        "specs": ['"size":"44mm"', '"size":"45mm"', '"gps":"Yes"', '"cellular":"LTE"'],
        "base_price": 15000,
        "images": [
            "https://images.unsplash.com/photo-1434493789847-2902a52dda56?w=400",
            "https://images.unsplash.com/photo-1579586337278-3befd40fd17a?w=400",
            "https://images.unsplash.com/photo-1617043786394-f977fa12eddf?w=400"
        ]
    },
    "Cameras": {
        "brands": ["Sony", "Canon", "Nikon", "Fujifilm", "Panasonic", "GoPro"],
        "models": ["A7 IV", "EOS R5", "Z6 II", "X-T5", "Lumix S5", "Hero 12"],
        "prefixes": ["Alpha", "EOS", "Z", "X-Series", "Lumix", "Hero"],
        "specs": ['"resolution":"24MP"', '"resolution":"33MP"', '"sensor":"Full-Frame"', '"video":"4K 60fps"'],
        "base_price": 80000,
        "images": [
            "https://images.unsplash.com/photo-1516035069371-29a1b244cc32?w=400",
            "https://images.unsplash.com/photo-1502920917128-1aa500764cbd?w=400",
            "https://images.unsplash.com/photo-1512790182412-b19e6d62bc39?w=400"
        ]
    },
    "Tablets": {
        "brands": ["Apple", "Samsung", "Lenovo", "Microsoft", "Xiaomi", "OnePlus"],
        "models": ["iPad Pro", "Galaxy Tab S9", "Pad 6", "Surface Pro 9", "Pad", "Tab P11"],
        "prefixes": ["iPad", "Galaxy Tab", "Surface Pro", "Pad", "Tab"],
        "specs": ['"size":"11-inch"', '"size":"12.9-inch"', '"storage":"256GB"', '"connectivity":"Wi-Fi"'],
        "base_price": 25000,
        "images": [
            "https://images.unsplash.com/photo-1544244015-0df4b3ffc6b0?w=400",
            "https://images.unsplash.com/photo-1589739900243-4b52cb8b13d2?w=400"
        ]
    }
}

sql_statements = []

sql_statements.append("DROP TABLE IF EXISTS product;\n")
sql_statements.append("CREATE TABLE product (id BIGINT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(255), description TEXT, price DECIMAL(10,2), category VARCHAR(255), image_url VARCHAR(500), tags VARCHAR(255), stock_quantity INT, rating DECIMAL(2,1), review_count INT, metadata TEXT);\n")
sql_statements.append("INSERT INTO product (name, description, price, category, image_url, tags, stock_quantity, rating, review_count, metadata) VALUES\n")

values = []
product_names = set()

# Seed specific hardcoded flagships to ensure demo queries work
flagships = [
    "('Apple MacBook Air M3', '13.6-inch Liquid Retina display, Apple M3 chip with 8-core CPU and 10-core GPU, 16GB Unified Memory, up to 18 hours battery life.', 114900.00, 'Laptops', 'https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=400', 'macbook,apple,laptop,m3,macbookair', 25, 4.9, 3100, '{\"brand\":\"Apple\",\"memory\":\"16GB\",\"storage\":\"512GB SSD\"}')",
    "('Lenovo LOQ Gaming Laptop', '15.6 FHD 144Hz display, Intel Core i5-13420H, NVIDIA RTX 4050 6GB, 16GB RAM, 512GB SSD, Windows 11.', 74990.00, 'Laptops', 'https://images.unsplash.com/photo-1593642632823-8f785ba67e45?w=400', 'laptop,lenovo,loq,gaming,rtx,windows', 40, 4.6, 1250, '{\"brand\":\"Lenovo\",\"memory\":\"16GB\",\"gpu\":\"RTX 4050\"}')",
    "('Apple iPhone 16 Pro Max', '6.9-inch Super Retina XDR display with ProMotion, A18 Pro chip, 48MP Fusion camera system with 5x Telephoto.', 144900.00, 'Smartphones', 'https://images.unsplash.com/photo-1592899677977-9c10ca588bbd?w=400', 'iphone,apple,smartphone,16promax,flagship', 30, 4.9, 4200, '{\"brand\":\"Apple\",\"storage\":\"256GB\",\"color\":\"Natural Titanium\"}')"
]
values.extend(flagships)
product_names.add("Apple MacBook Air M3")
product_names.add("Lenovo LOQ Gaming Laptop")
product_names.add("Apple iPhone 16 Pro Max")

for _ in range(1000):
    cat_name = random.choice(list(categories.keys()))
    cat = categories[cat_name]
    
    brand = random.choice(cat["brands"])
    model = random.choice(cat["models"])
    prefix = random.choice(cat["prefixes"])
    
    name = f"{brand} {prefix} {model}"
    if name in product_names:
        name = f"{brand} {prefix} {model} {random.randint(2023, 2026)} Edition"
    product_names.add(name)
    
    desc = f"Experience the premium {name}. High quality build, exceptional performance, and cutting-edge features. Perfect for your everyday needs."
    price = round(random.uniform(cat["base_price"] * 0.5, cat["base_price"] * 2.5), -2) - 10
    image_url = random.choice(cat["images"])
    tags = f"{cat_name.lower()},{brand.lower()},{model.lower()},{prefix.lower().replace(' ', '')}"
    stock = random.randint(0, 500)
    rating = round(random.uniform(3.5, 5.0), 1)
    review_count = random.randint(10, 5000)
    
    spec1 = random.choice(cat["specs"])
    spec2 = random.choice(cat["specs"])
    metadata = f'{{"brand":"{brand}",{spec1},{spec2}}}'
    
    val = f"('{name}', '{desc}', {price}, '{cat_name}', '{image_url}', '{tags}', {stock}, {rating}, {review_count}, '{metadata}')"
    values.append(val)

sql_statements.append(",\n".join(values) + ";")

with open("src/main/resources/data.sql", "w", encoding="utf-8") as f:
    f.write("".join(sql_statements))

print(f"Generated {len(values)} products into data.sql successfully.")
