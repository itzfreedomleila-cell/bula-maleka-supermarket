Put the database export here before submitting:

1. XAMPP: start Apache and MySQL, open http://localhost/phpmyadmin
2. Click "online_supermarket" on the left
3. Export tab -> Custom -> format SQL
   -> tick "Add CREATE DATABASE / USE statement" -> Go
4. Save the downloaded file in this folder as online_supermarket.sql

To load it on another computer: phpMyAdmin -> Import -> choose online_supermarket.sql -> Go.
(The app also creates the database and tables by itself when you click Run.)
