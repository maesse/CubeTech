del C:\wamp\www\current\* /Q
copy C:\wamp\www\base\* C:\wamp\www\current\
copy dist\CubeTech.jar C:\wamp\www\current\
copy dist\lib\* C:\wamp\www\current\
"C:\Program Files\Java\jdk1.6.0_22\bin\jarsigner" -keystore C:\Users\mads\keystore.jks -storepass Cubetech123 -keypass Cubetech123 C:\wamp\www\current\CubeTech.jar selfsigned