# EPAX-Cloud
A lightweight, modular, and easy-to-run cloud system.  

Welcome to **EPAX-Cloud**! This guide will walk you through installation and first-time setup step by step.

---

## 🚀 Installation Guide

### 1. Download the Required Files
Download the following from the official releases:

- **HTML Release (ZIP file)**
- **JAR File**

### 2. Install Java 21
EPAX-Cloud requires **Java 21**.  
We recommend **Debian**, but any OS works as long as Java 21 is installed.  

Install Java 21 using your OS package manager or download **OpenJDK 21** from the official website.

### 3. Create a Cloud Directory
Create a folder for EPAX-Cloud, for example:

```bash
mkdir epax-cloud
### 4. Place the Files

Move the JAR file into the directory.

Extract the HTML ZIP into the same directory.

Your directory structure should look like this:
epax-cloud/
├── EPX-CLOUD-SNAPSHOT-1.0.jar
└── html/
    ├── index.html
    └── ...
5. Create a Start Script

Create a file named run.sh (Linux/macOS) or run.bat (Windows).

Example run.sh:
#!/bin/bash
java -jar EPX-CLOUD-SNAPSHOT-1.0.jar
Make it executable on Linux/macOS:
chmod +x run.sh
6. Start the Cloud

Run the script:

On Linux/macOS:
./run.sh
On Windows:
  run.bat
7. Configure Your Database

Open the properties file inside the cloud directory.

Add your database connection details (used for account data).

In the running console, type:
stop
4. Restart the cloud using your script.

8. Access the Web Interface

Open your browser and navigate to your local IP, for example:
http://192.168.x.x
Create your account — and EPAX-Cloud is ready to use! 🎉
🧩 Optional: Pelican Panel

EPAX-Cloud can also be managed through Pelican Panel if you prefer a more user-friendly web interface.
