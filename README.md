# Bula Maleka Supermarket System (CS241 Assignment 2)

Spring Boot 4 · Java 21 · MySQL (XAMPP) · port 8080. All prices in FJD.

## Setting up on a new computer

1. Install XAMPP, open the XAMPP Control Panel and click **Start** next to **MySQL**.
2. Open this folder in NetBeans (**File → Open Project**), then **Clean and Build** (needs internet the first time).
3. Click **Run**. The app creates the `online_supermarket` database, all tables and the starting products by itself.

If your MySQL `root` user has a password, put it in `spring.datasource.password` in `application.properties`.
The file `database/online_supermarket.sql` is an export of our database with sample data. It is only for viewing or
importing in phpMyAdmin (**Import → choose file → Go**); the app does not need it to run.

Run the project in NetBeans and the browser opens the home page by itself.

## Logins

| Who | Email | Password |
|---|---|---|
| Staff (admin) | admin@supermarket.test | Admin12345 |
| Customer | register on the Log in page | |

Change the staff password in `application.properties` (`app.admin.*`).

## Pages

| Page | For | Modules |
|---|---|---|
| `index.html` | everyone | home, aisles, featured products |
| `login.html` | everyone | 1 Registration and Login |
| `cart.html` | everyone (cart needs login) | 2 Catalogue, 4 Shopping Cart |
| `checkout.html` | customers | 5 Payment, 6 Order Confirmation |
| `account.html` | customers | 6 Orders and receipts, 9 book and cancel delivery, 1 Logout and Deletion |
| `receipt.html` | customers and staff | 6 Receipt Creation (printable) |
| `admin.html` | staff | 3 Inventory, refunds, 9 drivers, deliveries and routes, money transfers |

## Architecture

Layered (controller, service, repository, model) and split into one package
per module under `net.codejava`:

```
account/   Module 1  User, PasswordHasher (PBKDF2), CurrentUser (session), AuthController
catalog/   Modules 2 and 3  Product, Category, CatalogService (search), InventoryService
cart/      Module 4  Cart, CartItem, CartService
payment/   Module 5  PaymentMethod, PaymentTransaction, LedgerEntry, PaymentGateway (simulated)
order/     Module 6  CustomerOrder, OrderItem, OrderService (receipts)
delivery/  Module 9  Delivery, Driver, DeliveryArea, RoutePlanner, DeliveryConflictDetector
common/    shared exceptions and JSON error handler, photo upload config, seed file reader
```

Modules talk through **service interfaces** and **Spring events** to keep coupling low:

- `PaymentSucceededEvent`: the order module creates, confirms and receipts the order.
- `PaymentRefundedEvent`: the order module records the refund.
- `AccountDeletedEvent`: the cart, payment and delivery modules clean up.

Starter data (7 aisles, 21 Fiji groceries, 12 Suva-area delivery zones,
4 drivers) is in `src/main/resources/seed/*.psv`, not hard-coded. It is
loaded only when the tables are empty. Every business rule (minimum order,
delivery fee, card fee, slot capacity, driver stops, speeds, cut-offs) is
in `application.properties`.

## Feature map

| Module | Feature | Where |
|---|---|---|
| 1 | Account Registration | `AccountServiceImpl.register`, `POST /api/auth/register` |
| 1 | Account Login | `login` (PBKDF2 check, locks after 5 wrong passwords), `POST /api/auth/login` |
| 1 | Account Logout | `POST /api/auth/logout` (session ended) |
| 1 | Account Deletion | `deleteAccount` (password check, personal data removed), `DELETE /api/auth/account` |
| 2 | Product List | `GET /api/catalog/products` |
| 2 | Product Categories | `GET /api/catalog/categories` |
| 2 | Advanced Search | multi-word keyword over name, description and aisle, ranked by relevance |
| 2 | Filtering | aisle, min/max price, in stock, sort (database query) |
| 2 | Description, Price and Photo | product dialog, `GET /api/catalog/products/{id}` |
| 3 | Product Addition / Info Edit / Removal | `InventoryServiceImpl`, staff Inventory tab, photo upload |
| 4 | Cart features (7) | `CartServiceImpl`, Shop page |
| 5 | Payment features (12) | `PaymentServiceImpl`, Checkout page, staff refunds and money tabs |
| 6 | Order Creation, Confirmation, Receipt | `OrderServiceImpl.onPaymentSucceeded`, receipt page |
| 9 | Schedule Delivery Order | `DeliveryServiceImpl.schedule`, My account page |
| 9 | Driver Assignment | `assignDriver`, staff Deliveries tab |
| 9 | Cancel Schedule | customer (before cut-off) and staff |
| 9 | Remove Appointed Driver | `removeDriver`; making a driver unavailable removes them from upcoming jobs |
| 9 | Confirm Delivery Schedule | `confirm` (needs a driver) |
| 9 | Route Scheduling | `RoutePlanner` nearest-neighbour route from the store, staff Routes tab |
| 9 | Estimated Delivery Time | travel time + 10 min per stop, updated whenever a route changes |
| 9 | Delivery Conflict Detection | `DeliveryConflictDetector`: full slot, too soon, double booking, driver at stop limit, route over the slot, unavailable driver |

## Payment test details (simulated, no real money)

Approved: 4242 4242 4242 4242 (Visa) or 5555 5555 5555 4444 (Mastercard).
Declined: 4000 0000 0000 0002. Use any future expiry and any 3-digit CVV.
Wallets: any 7 digits; a number ending 0000 has insufficient balance.

## Demo script (about 10 minutes)

1. **Module 1:** register a customer (show a validation error first), log out, log in.
2. **Module 2:** search "rice", filter Pantry under FJ$10, sort by price, open a product's details.
3. **Module 4:** add items, change a quantity, remove one, check out under FJ$20
   (minimum order), save and restore the cart.
4. **Module 5:** add an invalid card (errors), then a Visa and an M-PAiSA. Switch
   methods to show the card fee. Pay with the declined card, then the good card.
   Click "Send the same payment again" to show duplicate prevention.
5. **Module 6:** order confirmed with an order number, then View receipt and print.
6. **Module 9 (customer):** My account, Book delivery, choose an area, date and slot,
   and show the estimated arrival time.
7. **Staff:** log in as admin.
   - **Inventory:** add a product, edit its price and upload a photo, remove it.
   - **Deliveries:** Check, then Assign a driver. Assign more deliveries to one
     driver to trigger a conflict. Remove the driver, reassign, Confirm schedule,
     Mark delivered.
   - **Routes:** show the stop order and ETAs.
   - **Orders and refunds:** refund part of an order, track the payment status.
   - **Money transfers:** show the ledger and the store balance.
8. **Module 1:** delete the customer account (password required).

## Tests

Right-click the project, then **Test**. There are 12 test classes covering
accounts, password hashing, search, inventory, cart, payment validation and
calculation, payment processing, orders, route planning and delivery conflicts.

## Before submitting

Delete the `data` and `uploads` folders before zipping. The assignment
requires AI use to be acknowledged and does not allow AI output to be
submitted unchanged: review and adapt the code to your group's design, and
include your AI acknowledgement.
