==========

start working on this. check all the necessary resources are available like:

1)directory structure (check the directory structure directory wise to have more clear context by the end you need to have complete project structure knowledge i.e., where each file is where it is located in which folder what is the project directory how is it related to the project directory and much more)
 example command : Get-ChildItem -Path "E:\warp projects\kotlin mobile application" -Directory
2)supabase (use mcp to check the current status of the database and if all the necessary tables and datas available or if not what we need to create)
- supabase project url : https://supabase.com/dashboard/project/hfxdxxpmcemdjsvhsdcf


3)vercel api (use mcp and read related files to check what all endpoints are available and what needs to be created if needed)
-vercel base url :  https://andoid-app-kotlin.vercel.app/
-vercel project url : https://vercel.com/project3-f5839d18/andoid-app-kotlin/GEEpnmfSDrhjLmUrMVMuNZ7PJgFj


4) all android related installation are in "E:\Android" which will be useful while building, installing, debugging android apps we created

5) this is our project directory "E:\warp projects\kotlin mobile application"

7) this is the grocerycustomer user account you can use for testing : username = abcd@gmail.com and password = Password123

   this is the Admin app user account you can use for testing : username = admin@grocery.com and password = AdminPass123
   
   this is the Grocery Delivery app user account you can use for testing : username = driver@grocery.com and password = DriverPass123


8) always use proper authentication flow dont create any mockup data to bypass or don't create a quick fix that will make debugging harder and  that doen't solve the problem from the root

9) when you want to run the applicatoin or install it make sure you first uninstall the application (we need to always start with clean state) 

10) i need full fedged features don't make mokeup features to make the application running instead i need real end user features that are fully functional. don't add any quick fixes that doesn't address root problems

11) read "PROJECT_STATUS_AND_NEXT_STEPS.MD" and "API_INTEGRATION_GUIDE.MD" and "DESIGN_SYSTEM_GUIDE.MD"

12) these are working commands for GroceryCustomer app these are ran from "GroceryCustomer" folder : 

-> .\gradlew assembleDebug
-> & "E:\Android\Sdk\platform-tools\adb.exe" shell monkey -p com.grocery.customer -c android.intent.category.LAUNCHER 1
-> Start-Process powershell -ArgumentList "-NoExit", "-Command", "& 'E:\Android\Sdk\platform-tools\adb.exe' logcat | Select-String 'AuthRepositoryImpl|TokenStore|AuthInterceptor|OrderRepositoryImpl|CheckoutViewModel|HTTP|OkHttp|error|Error|fail|Fail|401|400'"
after everything is neccessary is available then start working on 

-> & "E:\Android\Sdk\platform-tools\adb.exe" install "app\build\outputs\apk\debug\app-debug.apk"
-> & "E:\Android\Sdk\platform-tools\adb.exe" uninstall com.grocery.customer.debug
-> Set-Location "E:\warp projects\kotlin mobile application\GroceryCustomer"; .\gradlew assembleDebug
-> & "E:\Android\Sdk\platform-tools\adb.exe" devices
-> Test-Path "E:\Android\Sdk\platform-tools\adb.exe"
-> & "E:\Android\Sdk\platform-tools\adb.exe" logcat -c

 

