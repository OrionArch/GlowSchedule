import os
from PIL import Image, ImageDraw

def make_background_transparent(img):
    width, height = img.size
    # Convert image to RGBA
    img = img.convert("RGBA")
    data = img.load()
    
    # BFS to flood-fill background white pixels
    queue = []
    visited = set()
    
    # Start from all four corners
    corners = [(0, 0), (width - 1, 0), (0, height - 1), (width - 1, height - 1)]
    for x, y in corners:
        queue.append((x, y))
        visited.add((x, y))
        
    while queue:
        cx, cy = queue.pop(0)
        r, g, b, a = data[cx, cy]
        
        # If the pixel is very light (almost white), make it transparent
        if r > 240 and g > 240 and b > 240:
            data[cx, cy] = (r, g, b, 0)
            
            # Queue 4-way neighbors
            for dx, dy in [(-1, 0), (1, 0), (0, -1), (0, 1)]:
                nx, ny = cx + dx, cy + dy
                if 0 <= nx < width and 0 <= ny < height and (nx, ny) not in visited:
                    visited.add((nx, ny))
                    queue.append((nx, ny))
                    
    return img

def create_adaptive_foreground(transparent_cat, size=512):
    # Adaptive foreground must be 512x512 (or 108x108 dp)
    # The icon itself should fit in the central safe zone (70%, e.g., 360x360)
    foreground = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    cat_size = int(size * 0.70)
    resized_cat = transparent_cat.resize((cat_size, cat_size), Image.Resampling.LANCZOS)
    
    # Center the cat
    offset = (size - cat_size) // 2
    foreground.paste(resized_cat, (offset, offset), resized_cat)
    return foreground

def create_legacy_icon(transparent_cat, size, shape="circle"):
    # Legacy icon background color is Morandi cream #FBF9F4
    bg_color = (0xFB, 0xF9, 0xF4, 0xFF)
    icon = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(icon)
    
    if shape == "circle":
        draw.ellipse([0, 0, size - 1, size - 1], fill=bg_color)
    else: # Squircle / rounded rectangle
        r = int(size * 0.2) # 20% corner radius
        draw.rounded_rectangle([0, 0, size - 1, size - 1], radius=r, fill=bg_color)
        
    # Center the cat inside the icon (occupying about 75% of the size)
    cat_size = int(size * 0.75)
    resized_cat = transparent_cat.resize((cat_size, cat_size), Image.Resampling.LANCZOS)
    offset = (size - cat_size) // 2
    icon.paste(resized_cat, (offset, offset), resized_cat)
    return icon

def main():
    workspace_dir = "/home/meiqilin/Documents/Project2026/android-app/sch-day"
    mascot_path = os.path.join(workspace_dir, "mockups/stitch_cat.png")
    
    if not os.path.exists(mascot_path):
        print(f"Error: Mascot image not found at {mascot_path}")
        return
        
    print("Loading mascot image...")
    mascot = Image.open(mascot_path)
    
    print("Running flood-fill to make background transparent...")
    transparent_cat = make_background_transparent(mascot)
    
    # 1. Save adaptive foreground
    res_dir = os.path.join(workspace_dir, "app/src/main/res")
    foreground_path = os.path.join(res_dir, "drawable/ic_launcher_foreground.png")
    print(f"Saving adaptive foreground to {foreground_path}...")
    # Delete XML foreground if it exists
    xml_foreground_path = os.path.join(res_dir, "drawable/ic_launcher_foreground.xml")
    if os.path.exists(xml_foreground_path):
        os.remove(xml_foreground_path)
        
    adaptive_fg = create_adaptive_foreground(transparent_cat)
    adaptive_fg.save(foreground_path, "PNG")
    
    # 2. Generate legacy icons for all densities
    densities = {
        "mipmap-mdpi": 48,
        "mipmap-hdpi": 72,
        "mipmap-xhdpi": 96,
        "mipmap-xxhdpi": 144,
        "mipmap-xxxhdpi": 192
    }
    
    for folder, size in densities.items():
        folder_path = os.path.join(res_dir, folder)
        os.makedirs(folder_path, exist_ok=True)
        
        # Square legacy webp
        square_icon = create_legacy_icon(transparent_cat, size, shape="square")
        square_path = os.path.join(folder_path, "ic_launcher.webp")
        # Delete existing files (in case they are PNG/WebP mismatch)
        for ext in [".png", ".webp"]:
            p = os.path.join(folder_path, "ic_launcher" + ext)
            if os.path.exists(p):
                os.remove(p)
        print(f"Saving square legacy icon to {square_path} ({size}x{size})...")
        square_icon.save(square_path, "WEBP")
        
        # Round legacy webp
        round_icon = create_legacy_icon(transparent_cat, size, shape="circle")
        round_path = os.path.join(folder_path, "ic_launcher_round.webp")
        for ext in [".png", ".webp"]:
            p = os.path.join(folder_path, "ic_launcher_round" + ext)
            if os.path.exists(p):
                os.remove(p)
        print(f"Saving round legacy icon to {round_path} ({size}x{size})...")
        round_icon.save(round_path, "WEBP")
        
    print("App icon generation completed successfully!")

if __name__ == "__main__":
    main()
