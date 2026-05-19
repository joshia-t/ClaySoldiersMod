"""Generate placeholder textures for Clay Legion mod."""
from PIL import Image
import os

# ──────────────────────────────────────────────────────────────────────────────
# Entity texture: 32x32, neutral grey shading so team colour tint produces
# proper 3D depth via vertex colour multiplication.
#
# UV layout for texOffs(u,v) box (w,h,d):
#   Down   : (u,          v)       .. (u+d,         v+d)
#   Up     : (u+d,        v)       .. (u+d+w,       v+d)    ← adjacent to Down, not after w gap
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
    fill(img, u,           v,     u+d,           v+d,   tint(BOTTOM, shade))  # down
    fill(img, u+d,         v,     u+d+w,         v+d,   tint(LIGHT,  shade))  # up
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

# Keep head front clean; no debug marker pixels.

out_dir = os.path.join("src", "main", "resources", "assets", "clay-legion", "textures", "entity")
os.makedirs(out_dir, exist_ok=True)
entity_img.save(os.path.join(out_dir, "clay_soldier.png"))
print("Entity texture saved:", os.path.join(out_dir, "clay_soldier.png"))

# ──────────────────────────────────────────────────────────────────────────────
# Mount texture: 32x32 shared atlas for low-poly mount model parts.
# ──────────────────────────────────────────────────────────────────────────────

MOUNT_BRIGHT = (215, 195, 168, 255)
MOUNT_LIGHT = (192, 170, 144, 255)
MOUNT_SIDE = (166, 142, 118, 255)
MOUNT_DARK = (132, 108, 88, 255)
MOUNT_BOTTOM = (145, 122, 99, 255)


def paint_box_custom(img, u, v, w, h, d, bright, light, side, dark, bottom, shade=1.0):
    fill(img, u,           v,     u+d,           v+d,   tint(bottom, shade))  # down
    fill(img, u+d,         v,     u+d+w,         v+d,   tint(light,  shade))  # up
    fill(img, u,           v+d,   u+d,           v+d+h, tint(side,   shade))  # left
    fill(img, u+d,         v+d,   u+d+w,         v+d+h, tint(bright, shade))  # front
    fill(img, u+d+w,       v+d,   u+d+w+d,       v+d+h, tint(side,   shade))  # right
    fill(img, u+d+w+d,     v+d,   u+d+w+d+w,     v+d+h, tint(dark,   shade))  # back


mount_img = Image.new('RGBA', (64, 64), EMPTY)

# BODY (texOffs 0,0) addBox(..., 6,4,8)
paint_box_custom(
    mount_img, 0, 0, 6, 4, 8,
    MOUNT_BRIGHT, MOUNT_LIGHT, MOUNT_SIDE, MOUNT_DARK, MOUNT_BOTTOM, 0.95
)

# HEAD (texOffs 0,12) addBox(..., 4,3,3)
paint_box_custom(
    mount_img, 0, 12, 4, 3, 3,
    MOUNT_BRIGHT, MOUNT_LIGHT, MOUNT_SIDE, MOUNT_DARK, MOUNT_BOTTOM, 1.00
)

# LEGS (texOffs 18,12 and 24,12) addBox(..., 1.4,3,1.4) -> texture uses 1x3x1 footprint
paint_box_custom(
    mount_img, 18, 12, 1, 3, 1,
    MOUNT_BRIGHT, MOUNT_LIGHT, MOUNT_SIDE, MOUNT_DARK, MOUNT_BOTTOM, 0.88
)
paint_box_custom(
    mount_img, 24, 12, 1, 3, 1,
    MOUNT_BRIGHT, MOUNT_LIGHT, MOUNT_SIDE, MOUNT_DARK, MOUNT_BOTTOM, 0.84
)

# WINGS (texOffs 0,18) 3x1x6 - slightly lighter for readability
paint_box_custom(
    mount_img, 0, 18, 3, 1, 6,
    (225, 222, 212, 255), (208, 205, 194, 255), (188, 184, 172, 255), (160, 155, 146, 255), (174, 170, 160, 255), 1.0
)

# SHELL (texOffs 14,18) 7x2x9 - darker green-brown turtle shell tone
paint_box_custom(
    mount_img, 14, 18, 7, 2, 9,
    (110, 126, 86, 255), (96, 112, 76, 255), (82, 95, 64, 255), (63, 74, 50, 255), (72, 84, 56, 255), 1.0
)

# EARS (texOffs 0,26) 3x2x1 - bunny accent
paint_box_custom(
    mount_img, 0, 26, 3, 2, 1,
    (236, 210, 220, 255), (220, 194, 203, 255), (198, 172, 181, 255), (164, 138, 147, 255), (178, 152, 160, 255), 1.0
)

# CREST (texOffs 10,26) 1x1x3 - gecko accent
paint_box_custom(
    mount_img, 10, 26, 1, 1, 3,
    (126, 178, 102, 255), (110, 160, 88, 255), (94, 140, 74, 255), (74, 112, 58, 255), (84, 126, 66, 255), 1.0
)

mount_img.save(os.path.join(out_dir, "clay_mount.png"))
print("Mount texture saved:", os.path.join(out_dir, "clay_mount.png"))

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
