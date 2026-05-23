SumUp Warsaw Accelerator Program
Software Engineer Intern
Home Task

🏙️ Home Task: Warsaw Beauty Salon Explorer
Context: You're building the foundation of a local services marketplace. The goal is to collect real data about hair/beauty salons in Warsaw and present it in a simple, usable web application - from data collection all the way to the browser.

🎯 What to Build
This is a full-stack task with three parts: data collection, backend API, and frontend UI.
As a result, it should be a single fully working application end-to-end.

Part 1 - Data Collection
Collect data on at least 100 Warsaw hair/beauty salons from any publicly available source (Google Maps, Booksy, Yelp, Facebook, Google Places API, etc. - your creativity is welcomed).
For each salon, capture as many fields as possible:
Table
Field
Required?
Name of the business
✅
Address
✅
District
✅
Phone number
⭐ nice to have
Website / social media link
⭐ nice to have
Services offered
⭐ nice to have
Price range
⭐ nice to have
Rating + number of reviews
⭐ nice to have

Rules:
Store the data in any format: SQLite, JSON, CSV, or a database of your choice

Part 2 - Backend API

1. Expose the collected data via a simple REST API:
   return the full list of salons (name, district, rating, price range)
   return full details for a single salon
   allow modification of single salon details

Part 3 - Frontend UI
A simple web interface that consumes the API:
A listing page - browse all salons, see key info at a glance (name, district, rating, price range)
Basic search or filter - by district or service type (at minimum one of these)

A detail view - click a salon to see its full details (address, phone, services, etc.)
Manual modification of details and saving changes in backend storage via API

📁 Deliverable
A link to publicly available GitHub repository (preferable) or uploaded archive containing:
Application code
A README.md explaining:
How to run the application
Technical solution and frameworks/tools used
What you'd improve with more time

🔧 Technology
Free choice - any language, framework, or tooling. Kotlin on the backend and React / Next.js on the frontend are a plus, but not required.

✅ What We Care About
Data quality - accurate, consistent, deduplicated records
Code quality - readable, structured, maintainable
Product thinking - does the UI actually help someone find a salon?
Documentation - can someone else run this from scratch?
We care more about how you think and how you work than whether you hit 100 records exactly.
⏱️ Time Expectation
~4–8 hours.
❓Be ready to discuss
Demo the app - show it running with real data
Why did you choose your data source?
How you handled missing or inconsistent data
How would you scale the data source to cover all of Poland?
