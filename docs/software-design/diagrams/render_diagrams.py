"""Render the five source-verified UML views to high-resolution PNG assets."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
import math

OUT = Path(__file__).parent
REG = r"C:\Windows\Fonts\arial.ttf"
BOLD = r"C:\Windows\Fonts\arialbd.ttf"
INK = "#183153"
BLUE = "#2363A6"
PALE = "#EFF6FC"
LIGHT = "#F6F8FB"
GRAY = "#5E6B78"
RED = "#A83232"
GREEN = "#24734B"


def canvas(w, h):
    im = Image.new("RGB", (w, h), "white")
    return im, ImageDraw.Draw(im)


def font(size, bold=False):
    return ImageFont.truetype(BOLD if bold else REG, size=size)


def centered(draw, xy, text, f, fill=INK, spacing=5):
    draw.multiline_text(xy, text, font=f, fill=fill, anchor="mm", align="center", spacing=spacing)


def arrow(draw, points, fill=INK, width=3, head=14, dashed=False, open_head=False):
    if dashed:
        for a, b in zip(points, points[1:]):
            x1, y1 = a
            x2, y2 = b
            length = math.hypot(x2 - x1, y2 - y1)
            if length == 0:
                continue
            ux, uy = (x2 - x1) / length, (y2 - y1) / length
            pos = 0.0
            while pos < max(0, length - head):
                end = min(pos + 10, max(0, length - head))
                draw.line((x1 + ux * pos, y1 + uy * pos, x1 + ux * end, y1 + uy * end), fill=fill, width=width)
                pos += 17
    else:
        draw.line(points, fill=fill, width=width, joint="curve")
    if len(points) > 1:
        (x1, y1), (x2, y2) = points[-2], points[-1]
        angle = math.atan2(y2 - y1, x2 - x1)
        left = (x2 - head * math.cos(angle - math.pi / 6), y2 - head * math.sin(angle - math.pi / 6))
        right = (x2 - head * math.cos(angle + math.pi / 6), y2 - head * math.sin(angle + math.pi / 6))
        if open_head:
            draw.line((left, (x2, y2), right), fill=fill, width=width, joint="curve")
        else:
            draw.polygon([(x2, y2), left, right], fill=fill)


def box(draw, rect, text, f, fill=PALE, outline=BLUE, radius=18, width=3, color=INK):
    draw.rounded_rectangle(rect, radius=radius, fill=fill, outline=outline, width=width)
    centered(draw, ((rect[0] + rect[2]) // 2, (rect[1] + rect[3]) // 2), text, f, color)


def ellipse(draw, rect, text, f, fill="white", outline=INK, width=3):
    draw.ellipse(rect, fill=fill, outline=outline, width=width)
    centered(draw, ((rect[0] + rect[2]) // 2, (rect[1] + rect[3]) // 2), text, f)


def diamond(draw, cx, cy, w, h, text, f, fill="white", outline=BLUE):
    pts = [(cx, cy - h // 2), (cx + w // 2, cy), (cx, cy + h // 2), (cx - w // 2, cy)]
    draw.polygon(pts, fill=fill, outline=outline)
    draw.line(pts + [pts[0]], fill=outline, width=3, joint="curve")
    centered(draw, (cx, cy), text, f)
    return pts


def actor(draw, x, y, label, f, color=INK):
    # UML stick actor with head, body, arms, and legs.
    r = 19
    draw.ellipse((x - r, y - 60 - r, x + r, y - 60 + r), fill="white", outline=color, width=4)
    draw.line((x, y - 40, x, y + 34), fill=color, width=4)
    draw.line((x - 35, y - 15, x + 35, y - 15), fill=color, width=4)
    draw.line((x, y + 34, x - 31, y + 84), fill=color, width=4)
    draw.line((x, y + 34, x + 31, y + 84), fill=color, width=4)
    centered(draw, (x, y + 113), label, f, color)


def use_case():
    w, h = 1800, 1840
    im, d = canvas(w, h)
    d.rectangle((340, 105, 1450, 1740), outline=GRAY, width=3)
    centered(d, (895, 135), "BLUE THREAD / Shopee", font(34, True))
    left = [
        (220, "Browse catalog"), (365, "Register / log in"), (510, "Manage cart"),
        (655, "Checkout"), (800, "View own orders"), (945, "Cancel pending order"),
        (1090, "Confirm receipt"), (1235, "Manage wishlist"), (1380, "Submit product review"),
        (1525, "Read notifications"),
    ]
    right = [
        (285, "Manage products\nand variants"), (485, "Manage categories"),
        (685, "Manage order\ndelivery status"), (885, "Confirm bank transfer"),
        (1085, "Activate / deactivate\nusers"), (1285, "View statistics"),
        (1485, "View / add bank accounts"),
    ]
    # Guest and Customer associations to customer use cases.
    actor(d, 145, 280, "Guest", font(25, True))
    actor(d, 145, 1000, "Customer", font(25, True))
    actor(d, 1645, 1000, "Administrator", font(25, True))
    # Boundary-side edges of the use-case ellipses.
    left_bounds = []
    for y, label in left:
        rect = (520, y - 48, 940, y + 48)
        ellipse(d, rect, label, font(25), fill="#FFFFFF")
        left_bounds.append((y, rect))
    right_bounds = []
    for y, label in right:
        rect = (1010, y - 52, 1375, y + 52)
        ellipse(d, rect, label, font(24), fill="#FFFFFF")
        right_bounds.append((y, rect))
    # Draw associations behind labels / actors so symbols remain clear.
    for idx in [0, 1]:
        y, rect = left_bounds[idx]
        d.line((176, 280, rect[0], y), fill="#738294", width=2)
    for y, rect in left_bounds:
        d.line((177, 1000, rect[0], y), fill="#738294", width=2)
    for y, rect in right_bounds:
        d.line((rect[2], y, 1598, 1000), fill="#738294", width=2)
    # Repaint actors and ellipse outlines/text over association endpoints.
    actor(d, 145, 280, "Guest", font(25, True))
    actor(d, 145, 1000, "Customer", font(25, True))
    actor(d, 1645, 1000, "Administrator", font(25, True))
    for y, label in left:
        ellipse(d, (520, y - 48, 940, y + 48), label, font(25), fill="#FFFFFF")
    for y, label in right:
        ellipse(d, (1010, y - 52, 1375, y + 52), label, font(24), fill="#FFFFFF")
    d.rounded_rectangle((385, 1760, 1405, 1818), radius=10, fill=LIGHT, outline="#C8D1DC", width=2)
    centered(d, (895, 1789), "Actors and capabilities reflect public routes and USER / ADMIN access rules.", font(22), GRAY)
    im.save(OUT / "use-case.png", dpi=(240, 240))


def activity():
    w, h = 2200, 1720
    im, d = canvas(w, h)
    # UML activity partitions (swimlanes).
    bounds = [0, 500, 1050, 2200]
    for x in bounds[1:-1]:
        d.line((x, 0, x, 1400), fill="#9AA8B6", width=2)
    for y in [320, 650, 1680]:
        d.line((0, y, w, y), fill="#9AA8B6", width=2)
    labels = [(250, "Customer"), (775, "HTTP API / Security"), (1625, "OrderService + persistence")]
    for x, label in labels:
        centered(d, (x, 35), label, font(27, True), BLUE)
    # Initial node and customer action.
    d.ellipse((82, 112, 122, 152), fill="black")
    box(d, (145, 100, 475, 205), "Submit checkout details", font(25), fill="#FCEFF8", outline="#C3519A")
    arrow(d, [(122, 132), (145, 132)], width=3)
    # API call and guard.
    box(d, (550, 365, 850, 485), "POST /api/orders\n@Valid request", font(24))
    arrow(d, [(475, 155), (520, 155), (520, 425), (550, 425)])
    diamond(d, 960, 425, 180, 145, "USER role\nand valid DTO?", font(22))
    arrow(d, [(850, 425), (865, 425)])
    # No branch to API response error and final activity.
    box(d, (825, 535, 1040, 625), "Return 401 / 403\nor 400 validation", font(19), fill="#FFF2F0", outline=RED)
    arrow(d, [(960, 498), (960, 535)], fill=RED)
    d.text((976, 507), "no", font=font(20, True), fill=RED)
    d.ellipse((964, 645, 1017, 698), outline="black", width=4)
    d.ellipse((974, 655, 1007, 688), fill="black")
    arrow(d, [(932, 625), (932, 670), (964, 670)], fill=RED)
    # yes to service.
    d.text((1044, 452), "yes", font=font(20, True), fill=GREEN)
    box(d, (1090, 700, 1405, 805), "Find user by email", font(24))
    arrow(d, [(1055, 425), (1080, 425), (1080, 752), (1090, 752)], fill=GREEN)
    # Guard the service-level user lookup before locking the cart.
    diamond(d, 1530, 752, 210, 140, "User exists?", font(22))
    arrow(d, [(1405, 752), (1425, 752)])
    box(d, (1660, 685, 1935, 800), "Return 401\nuser not found", font(23), fill="#FFF2F0", outline=RED)
    arrow(d, [(1635, 752), (1660, 752)], fill=RED)
    d.text((1638, 720), "no", font=font(20, True), fill=RED)
    d.ellipse((1980, 716, 2033, 769), outline="black", width=4)
    d.ellipse((1990, 726, 2023, 759), fill="black")
    arrow(d, [(1935, 742), (1980, 742)], fill=RED)
    # Existing user continues to lock the cart and check it is non-empty.
    d.text((1540, 845), "yes", font=font(20, True), fill=GREEN)
    box(d, (1090, 915, 1405, 1020), "Lock user cart", font(24))
    arrow(d, [(1530, 822), (1530, 875), (1247, 875), (1247, 915)], fill=GREEN)
    diamond(d, 1530, 967, 210, 140, "Cart exists\nand is non-empty?", font(21))
    arrow(d, [(1405, 967), (1425, 967)])
    box(d, (1660, 910, 1935, 1040), "Return 400\nempty-cart error", font(23), fill="#FFF2F0", outline=RED)
    arrow(d, [(1635, 967), (1660, 967)], fill=RED)
    d.text((1638, 935), "no", font=font(20, True), fill=RED)
    d.ellipse((1980, 948, 2033, 1001), outline="black", width=4)
    d.ellipse((1990, 958, 2023, 991), fill="black")
    arrow(d, [(1935, 975), (1980, 975)], fill=RED)
    # yes into variant validation activity and stock guard.
    d.text((1540, 1060), "yes", font=font(20, True), fill=GREEN)
    box(d, (1080, 1120, 1510, 1275), "For each cart line (variant ID order):\nlock variant; require variant exists,\nproduct ACTIVE and quantity <= stock", font(21))
    arrow(d, [(1530, 1037), (1530, 1080), (1295, 1080), (1295, 1120)], fill=GREEN)
    diamond(d, 1660, 1197, 205, 140, "All lines\nvalid?", font(22))
    arrow(d, [(1510, 1197), (1557, 1197)])
    box(d, (1790, 1112, 2110, 1242), "Return 400\ncheckout error", font(23), fill="#FFF2F0", outline=RED)
    arrow(d, [(1762, 1197), (1790, 1197)], fill=RED)
    d.text((1765, 1163), "no", font=font(20, True), fill=RED)
    d.ellipse((2130, 1170, 2183, 1223), outline="black", width=4)
    d.ellipse((2140, 1180, 2173, 1213), fill="black")
    arrow(d, [(2110, 1197), (2130, 1197)], fill=RED)
    # success creates order; payment handling is conditional.
    box(d, (1050, 1340, 1470, 1485), "Compute total; build order and items;\ncreate Payment; decrement stock; save order", font(20), fill="#EDF8F1", outline=GREEN)
    arrow(d, [(1660, 1267), (1660, 1310), (1260, 1310), (1260, 1340)], fill=GREEN)
    d.text((1672, 1280), "yes", font=font(20, True), fill=GREEN)
    diamond(d, 1580, 1412, 175, 130, "BANKING?", font(21))
    arrow(d, [(1470, 1412), (1492, 1412)], fill=GREEN)
    box(d, (1735, 1325, 2120, 1415), "Create transfer transaction", font(21), fill="#EDF8F1", outline=GREEN)
    arrow(d, [(1667, 1412), (1700, 1412), (1700, 1370), (1735, 1370)], fill=GREEN)
    d.text((1703, 1395), "yes", font=font(18, True), fill=GREEN)
    box(d, (1510, 1555, 1945, 1650), "Clear cart; update timestamp;\nreturn OrderResponse", font(21), fill="#EDF8F1", outline=GREEN)
    arrow(d, [(1580, 1477), (1580, 1555)], fill=GREEN)
    d.text((1592, 1500), "no", font=font(18), fill=GRAY)
    arrow(d, [(1928, 1415), (1928, 1515), (1790, 1515), (1790, 1555)], fill=GREEN)
    arrow(d, [(1945, 1602), (2035, 1602)], fill=GREEN)
    d.ellipse((2035, 1576, 2090, 1631), outline="black", width=4)
    d.ellipse((2046, 1587, 2079, 1620), fill="black")
    im.save(OUT / "activity-checkout.png", dpi=(240, 240))


def state_diagram():
    w, h = 1800, 1370
    im, d = canvas(w, h)
    centered(d, (900, 52), "OrderStatus state machine", font(37, True))
    # Normal order path across top row.
    d.ellipse((70, 180, 116, 226), fill="black")
    box(d, (180, 145, 405, 260), "PENDING", font(29, True), fill="#FFF8DD", outline="#B28A00", radius=22)
    arrow(d, [(116, 203), (180, 203)])
    box(d, (570, 145, 800, 260), "CONFIRMED", font(29, True), fill="#EAF7EC", outline="#4D995B", radius=22)
    arrow(d, [(405, 203), (570, 203)])
    centered(d, (488, 158), "confirm", font(19), GRAY)
    box(d, (985, 145, 1215, 260), "SHIPPING", font(29, True), fill="#EEF4FE", outline="#4A7EB8", radius=22)
    arrow(d, [(800, 203), (985, 203)])
    centered(d, (892, 158), "startDelivery", font(19), GRAY)
    box(d, (1390, 145, 1620, 260), "DELIVERED", font(29, True), fill="#EAF8F3", outline="#43A787", radius=22)
    arrow(d, [(1215, 203), (1390, 203)])
    centered(d, (1302, 158), "markDelivered", font(19), GRAY)
    box(d, (1390, 440, 1620, 555), "COMPLETED", font(29, True), fill="#EAF8F3", outline="#43A787", radius=22)
    arrow(d, [(1505, 260), (1505, 440)])
    centered(d, (1595, 345), "confirmReceipt\n(owner)", font(19), GRAY)
    d.ellipse((1695, 474, 1755, 534), outline="black", width=4)
    d.ellipse((1707, 486, 1743, 522), fill="black")
    arrow(d, [(1620, 498), (1695, 504)])
    # Cancel branch is only available from pending.
    box(d, (180, 470, 405, 585), "CANCELLED", font(29, True), fill="#FFF0EF", outline="#C15D54", radius=22)
    arrow(d, [(292, 260), (292, 470)], fill=RED)
    centered(d, (410, 360), "cancel / cancelByCustomer\n(PENDING only)", font(19), RED)
    d.ellipse((70, 495, 130, 555), outline="black", width=4)
    d.ellipse((82, 507, 118, 543), fill="black")
    arrow(d, [(180, 526), (130, 526)], fill=RED)
    # Notes for code paths and related enums.
    d.rounded_rectangle((520, 690, 1290, 925), radius=14, fill="#F7F9FC", outline="#7C8FA4", width=2)
    d.text((550, 715), "Observed BANKING confirmation path", font=font(25, True), fill=INK)
    d.multiline_text((550, 758), "BANKING confirmation requires a PENDING transaction, PENDING order, and\nBANKING payment. It sets the order to CONFIRMED and writes a notification.\nOrderService.confirm uses the same PENDING state guard.", font=font(22), fill=INK, spacing=7)
    d.rounded_rectangle((380, 970, 1430, 1265), radius=14, fill=LIGHT, outline="#A9B5C2", width=2)
    d.text((550, 995), "Related status enums (not order states)", font=font(25, True), fill=INK)
    d.multiline_text((410, 1040), "PaymentStatus: PENDING, PAID, FAILED; BANKING cancellation sets FAILED.\nPaymentTransaction: PENDING, CONFIRMED, REJECTED; cancellation sets REJECTED.\nSuccessful admin payment confirmation sets CONFIRMED / PAID.", font=font(20), fill=INK, spacing=7)
    # Transition rules footnote.
    d.text((70, 1305), "Evidence: OrderStatus.java; OrderService.java; PaymentTransactionService.java; PaymentStatus.java.", font=font(21), fill=GRAY)
    im.save(OUT / "state-order-payment.png", dpi=(240, 240))


def sequence():
    w, h = 2200, 1860
    im, d = canvas(w, h)
    centered(d, (1100, 42), "Login sequence", font(37, True))
    xs = [135, 405, 680, 960, 1240, 1510, 1780, 2060]
    names = ["Customer", "AuthPage.jsx", "authService.js\n+ api.js", "Security +\nAuthController", "AuthService", "UserRepository\n+ database", "PasswordEncoder", "JwtService"]
    tops = []
    for x, name in zip(xs, names):
        top = (x - 112, 88, x + 112, 182)
        d.rounded_rectangle(top, radius=8, fill="#F8FAFC", outline=BLUE, width=3)
        centered(d, (x, 135), name.replace("+ api.js", "/ api.js"), font(22, True))
        d.line((x, 182, x, 1815), fill="#8091A3", width=2)
        tops.append(top)
    # Dashed lifelines.
    for x in xs:
        for y in range(182, 1815, 20):
            d.line((x, y, x, min(y + 10, 1815)), fill="#8091A3", width=2)
    def msg(a, b, y, label, response=False, color=INK, xshift=0):
        xa, xb = xs[a] + xshift, xs[b] + xshift
        if a == b:
            # self-call loop
            arrow(d, [(xa, y), (xa + 60, y), (xa + 60, y + 45), (xa, y + 45)], fill=color, width=2, head=10, dashed=response)
            centered(d, (xa + 105, y + 21), label, font(19), color)
        else:
            arrow(d, [(xa, y), (xb, y)], fill=color, width=2, head=10, dashed=response, open_head=response)
            centered(d, ((xa + xb) // 2, y - 18), label, font(19), color)
    # Main interactions.
    msg(0, 1, 230, "Enter email / password")
    msg(1, 2, 290, "loginAccount(email, password)")
    msg(2, 3, 350, "POST /api/auth/login (JSON)")
    msg(3, 4, 410, "login(LoginRequest)")
    msg(4, 5, 470, "findByEmailIgnoreCase\n(trim(email))")
    msg(5, 4, 525, "Optional<User>", True)
    # Alternative frame starts around service/database interaction.
    d.rectangle((80, 555, 2150, 1795), outline=BLUE, width=3)
    d.rectangle((80, 555, 205, 595), fill="#EEF4FE", outline=BLUE, width=2)
    centered(d, (142, 575), "alt", font(20, True), BLUE)
    d.text((220, 562), "Login outcomes", font=font(18), fill=INK)
    # User not found branch.
    d.line((80, 625, 2150, 625), fill=BLUE, width=2)
    d.text((220, 600), "[user not found]", font=font(18, True), fill=GRAY)
    msg(4, 3, 655, "401 Unauthorized", True, RED)
    msg(3, 2, 695, "401 JSON error", True, RED)
    msg(2, 1, 735, "Reject promise", True, RED)
    msg(1, 0, 775, "Show error toast", True, RED)
    # Password mismatch branch.
    d.line((80, 810, 2150, 810), fill=BLUE, width=2)
    d.text((220, 815), "[user found; password mismatch]", font=font(18, True), fill=GRAY)
    msg(4, 6, 855, "matches(rawPassword, hash)")
    msg(6, 4, 895, "false", True)
    msg(4, 3, 935, "401 Unauthorized", True, RED)
    msg(3, 2, 975, "401 JSON error", True, RED)
    msg(2, 1, 1015, "Reject promise", True, RED)
    msg(1, 0, 1055, "Show error toast", True, RED)
    # Correct password, but inactive account.
    d.line((80, 1100, 2150, 1100), fill=BLUE, width=2)
    d.text((220, 1105), "[password matched; account inactive]", font=font(18, True), fill=GRAY)
    msg(4, 6, 1140, "matches(rawPassword, hash)")
    msg(6, 4, 1180, "true", True)
    msg(4, 3, 1220, "403 Forbidden", True, RED)
    msg(3, 2, 1260, "403 JSON error", True, RED)
    msg(2, 1, 1300, "Reject promise", True, RED)
    msg(1, 0, 1340, "Show error toast", True, RED)
    # Correct password and active account.
    d.line((80, 1390, 2150, 1390), fill=BLUE, width=2)
    d.text((220, 1395), "[password matched; account active]", font=font(18, True), fill=GRAY)
    msg(4, 6, 1430, "matches(rawPassword, hash)")
    msg(6, 4, 1470, "true", True)
    msg(4, 7, 1510, "createToken(user)")
    msg(7, 4, 1550, "signed JWT", True)
    msg(4, 3, 1590, "AuthResponse(token, userId, role)", True, GREEN)
    msg(3, 2, 1630, "200 AuthResponse", True, GREEN)
    msg(2, 1, 1670, "Resolve AuthResponse", True, GREEN)
    msg(1, 1, 1710, "saveSession; safe redirect / role route")
    msg(1, 0, 1760, "Show authenticated page", True, GREEN)
    # Refine participant labels with high-contrast header text.
    im.save(OUT / "sequence-login.png", dpi=(240, 240))


def component():
    w, h = 1900, 1400
    im, d = canvas(w, h)
    centered(d, (950, 47), "Component diagram — BLUE THREAD / Shopee", font(36, True))
    # Package boundaries.
    d.rounded_rectangle((35, 120, 520, 1080), radius=14, fill="#FBFCFE", outline="#7A8A9B", width=3)
    d.rounded_rectangle((560, 120, 1865, 1080), radius=14, fill="#FBFCFE", outline="#7A8A9B", width=3)
    d.rectangle((35, 120, 520, 180), fill="#EEF4FE", outline="#7A8A9B", width=2)
    d.rectangle((560, 120, 1865, 180), fill="#EEF4FE", outline="#7A8A9B", width=2)
    centered(d, (277, 150), "Frontend — React / Vite", font(27, True))
    centered(d, (1212, 150), "Backend — Spring Boot", font(27, True))

    def component_box(x, y, w, h, title, stereotype="component", fill="white"):
        d.rounded_rectangle((x, y, x + w, y + h), radius=8, fill=fill, outline=BLUE, width=3)
        # UML 2 component icon: two small rectangles attached to the left edge.
        d.rectangle((x - 12, y + 24, x + 3, y + 42), fill="white", outline=BLUE, width=2)
        d.rectangle((x - 12, y + 48, x + 3, y + 66), fill="white", outline=BLUE, width=2)
        centered(d, (x + w // 2, y + h // 2 - 16), f"«{stereotype}»\n{title}", font(22, True))
    component_box(110, 260, 325, 125, "React pages / features")
    component_box(110, 485, 325, 125, "services/*")
    component_box(110, 710, 325, 150, "api.js\nHTTP / JSON / session JWT")
    # Frontend dependencies.
    arrow(d, [(272, 385), (272, 485)], fill=BLUE, width=3, head=14, dashed=True, open_head=True)
    centered(d, (355, 435), "calls", font(19), GRAY)
    arrow(d, [(272, 610), (272, 710)], fill=BLUE, width=3, head=14, dashed=True, open_head=True)
    centered(d, (355, 660), "calls", font(19), GRAY)

    component_box(640, 260, 340, 140, "JWT filter +\nSecurityConfig")
    component_box(1070, 260, 340, 140, "REST controllers")
    component_box(1510, 250, 310, 150, "Business services")
    component_box(1510, 500, 310, 150, "PaymentTransactionService")
    component_box(1060, 760, 355, 145, "Spring Data JPA\nrepositories")
    component_box(640, 760, 320, 145, "JPA entities")
    # Backend and frontend-to-backend dependencies.
    arrow(d, [(980, 330), (1070, 330)], fill=BLUE, width=3, head=14, dashed=True, open_head=True)
    centered(d, (1025, 295), "secured", font(18), GRAY)
    arrow(d, [(1410, 330), (1510, 330)], fill=BLUE, width=3, head=14, dashed=True, open_head=True)
    centered(d, (1460, 295), "delegates", font(18), GRAY)
    arrow(d, [(1665, 400), (1665, 500)], fill=BLUE, width=3, head=14, dashed=True, open_head=True)
    centered(d, (1750, 452), "BANKING checkout", font(18), GRAY)
    arrow(d, [(1510, 360), (1450, 360), (1450, 830), (1415, 830)], fill=BLUE, width=3, head=14, dashed=True, open_head=True)
    centered(d, (1460, 720), "queries / saves", font(18), GRAY)
    arrow(d, [(1510, 575), (1460, 575), (1460, 830), (1415, 830)], fill=BLUE, width=3, head=14, dashed=True, open_head=True)
    arrow(d, [(1060, 830), (960, 830)], fill=BLUE, width=3, head=14, dashed=True, open_head=True)
    centered(d, (1010, 795), "maps", font(18), GRAY)
    # The REST API is provided by the controllers; the database remains outside the backend package.
    component_box(1205, 1120, 460, 115, "MySQL-compatible database", "database", fill="#F6F8FB")
    arrow(d, [(435, 785), (530, 785), (530, 330), (640, 330)], fill=GREEN, width=3, head=15, dashed=True, open_head=True)
    centered(d, (545, 690), "HTTP JSON /api", font(18), GREEN)
    arrow(d, [(1415, 830), (1500, 830), (1500, 1120)], fill=GREEN, width=3, head=15, dashed=True, open_head=True)
    centered(d, (1560, 1075), "JPA / JDBC", font(18), GREEN)
    d.rounded_rectangle((560, 1270, 1865, 1350), radius=10, fill=LIGHT, outline="#C8D1DC", width=2)
    centered(d, (1212, 1310), "No external payment gateway or Univer client is shown: none was found in the inspected source.", font(19), GRAY)
    im.save(OUT / "component-architecture.png", dpi=(240, 240))


if __name__ == "__main__":
    use_case()
    activity()
    state_diagram()
    sequence()
    component()
