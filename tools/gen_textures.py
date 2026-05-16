"""Generate placeholder textures for Clay Legion mod."""
from PIL import Image
import os

# ──────────────────────────────────────────────────────────────────────────────
# Entity texture: 32x32, neutral grey shading so team colour tint produces
# proper 3D depth via vertex colour multiplication.
#
# UV layout for texOffs(u,v) box (w,h,d):
#   Bottom : (u,          v)       .. (u+d,         v+d)
#   Top    : (u+d+w,      v)       .. (u+d+w+d,     v+d)
#   Left   : (u,          v+d)     .. (u+d,         v+d+h)
#   Front  : (u+d,        v+d)     .. (u+d+w,       v+d+h)
#   Right  : (u+d+w,      v+d)     .. (u+d+w+d,     v+d+h)
#   Back   : (u+d+w+d,    v+d)     .. (u+d+w+d+w,   v+d+h)
# ──────────────────────────────────────────────────────────────────────────────

BRIGHT = (255, 255, 255, 255)  # front face
LIGHT  = (220, 220, 220, 255)  # top
SIDE   = (190, 190, 190, 255)  # left / right
DARK   = (155, 155, 155, 255)  # back
BOTTOM = (170, 170, 170, 255)  # bottom
EMPTY  = (0,   0,   0,   0  )  # transparent
MARK   = (40,  40,  40, 255)   # visual marker accents


def fill(img, x0, y0, x1, y1, color):
    for y in range(y0, y1):
        for x in range(x0, x1):
            img.putpixel((x, y), color)


def tint(c, factor):
    return (int(c[0] * factor), int(c[1] * factor), int(c[2] * factor), c[3])


def paint_box(img, u, v, w, h, d, shade=1.0):
    fill(img, u,           v,     u+d,           v+d,   tint(BOTTOM, shade))  # bottom
    fill(img, u+d+w,       v,     u+d+w+d,       v+d,   tint(LIGHT,  shade))  # top
    fill(img, u,           v+d,   u+d,           v+d+h, tint(SIDE,   shade))  # left
    fill(img, u+d,         v+d,   u+d+w,         v+d+h, tint(BRIGHT, shade))  # front
    fill(img, u+d+w,       v+d,   u+d+w+d,       v+d+h, tint(SIDE,   shade))  # right
    fill(img, u+d+w+d,     v+d,   u+d+w+d+w,     v+d+h, tint(DARK,   shade))  # back


entity_img = Image.new('RGBA', (32, 32), EMPTY)

# HEAD      (w=3,h=3,d=3) texOffs(0, 0)
paint_box(entity_img, u=0,  v=0,  w=3, h=3, d=3, shade=1.00)
# BODY      (w=4,h=4,d=2) texOffs(0, 8)
paint_box(entity_img, u=0,  v=8,  w=4, h=4, d=2, shade=0.90)
# RIGHT_ARM (w=2,h=5,d=2) texOffs(0,16)
paint_box(entity_img, u=0,  v=16, w=2, h=5, d=2, shade=0.82)
# LEFT_ARM  (w=2,h=5,d=2) texOffs(8,16)
paint_box(entity_img, u=8,  v=16, w=2, h=5, d=2, shade=0.82)
# RIGHT_LEG (w=2,h=5,d=2) texOffs(16,16)
paint_box(entity_img, u=16, v=16, w=2, h=5, d=2, shade=0.72)
# LEFT_LEG  (w=2,h=5,d=2) texOffs(24,16)
paint_box(entity_img, u=24, v=16, w=2, h=5, d=2, shade=0.72)

# Head front eye pixels for orientation readability.
entity_img.putpixel((4, 4), MARK)
entity_img.putpixel((5, 4), MARK)

out_dir = os.path.join("src", "main", "resources", "assets", "clay-legion", "textures", "entity")
os.makedirs(out_dir, exist_ok=True)
entity_img.save(os.path.join(out_dir, "clay_soldier.png"))
print("Entity texture saved:", os.path.join(out_dir, "clay_soldier.png"))

# ──────────────────────────────────────────────────────────────────────────────
# Item texture: 16x16 clay-doll silhouette
# ──────────────────────────────────────────────────────────────────────────────

CLAY  = (185, 148, 112, 255)
CLAY2 = (160, 120,  90, 255)
SHADE = (110,  80,  55, 255)
TRANS = (0, 0, 0, 0)

item = Image.new('RGBA', (16, 16), TRANS)


def p(x, y, c):
    item.putpixel((x, y), c)


# Head 4x4 at (6,1)-(9,4)
for x in range(6, 10):
    for y in range(1, 5):
        p(x, y, CLAY if (x in (6, 9) or y in (1, 4)) else CLAY2)
p(7, 2, SHADE)   # left eye
p(8, 2, SHADE)   # right eye

# Body 4x4 at (6,5)-(9,8)
for x in range(6, 10):
    for y in range(5, 9):
        p(x, y, CLAY2 if x in (6, 9) else CLAY)

# Arms (1-wide, y=5-7)
for y in range(5, 8):
    p(5, y, CLAY2)
    p(10, y, CLAY2)

# Legs (1x3 each, y=9-11)
for y in range(9, 12):
    p(6, y, CLAY2)
    p(7, y, CLAY)
    p(8, y, CLAY)
    p(9, y, CLAY2)

item_dir = os.path.join("src", "main", "resources", "assets", "clay-legion", "textures", "item")
os.makedirs(item_dir, exist_ok=True)
item.save(os.path.join(item_dir, "soldier_doll.png"))
print("Item texture saved:", os.path.join(item_dir, "soldier_doll.png"))
