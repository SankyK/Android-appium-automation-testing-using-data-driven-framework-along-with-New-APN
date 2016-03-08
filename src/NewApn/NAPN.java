package NewApn;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.AndroidKeyCode;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.openqa.selenium.By;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.testng.annotations.Test;

public class NAPN {
	
	//WebDriver driver;
	AndroidDriver driver;
  @Test
  public void APN() throws Exception {
	  

		//Set up desired capabilities and pass the Android app-activity and app-package to Appium
			DesiredCapabilities capabilities = new DesiredCapabilities();
			//capabilities.setCapability("BROWSER_NAME", "Android");
			capabilities.setCapability("VERSION", "5.0.1"); 
			capabilities.setCapability("deviceName","Galaxy S4");
			capabilities.setCapability("platformName","Android");
			capabilities.setCapability("appActivity",".Settings");
		 capabilities.setCapability("appPackage","com.android.settings");
		 //  capabilities.setCapability("appPackage", "com.android.calculator2");
		// This package name of your app (you can get it from apk info app)
			//capabilities.setCapability("appActivity","com.android.calculator2.Calculator"); // This is Launcher activity of your app (you can get it from apk info app)
		//Create RemoteWebDriver instance and connect to the Appium server
		 //It will launch the Calculator App in Android Device using the configurations specified in Desired Capabilities
		   try {
			driver = new AndroidDriver(new URL("http://192.168.0.106:4722/wd/hub"), capabilities);
			System.out.println("Driver found");
		
		   
		   driver.findElement(By.xpath("//android.widget.TextView[contains(@resource-id,'android:id/title') and @text='More networks']")).click();
		   Thread.sleep(2000);
		   
		   driver.findElement(By.xpath("//android.widget.TextView[contains(@resource-id,'android:id/title') and @text='Mobile networks']")).click();
		   Thread.sleep(2000);
		   
		   driver.findElement(By.xpath("//android.widget.TextView[contains(@resource-id,'android:id/title') and @text='Access Point Names']")).click();
		   Thread.sleep(2000);
		   
		   driver.findElement(By.xpath("//android.widget.Button[@index='0']")).click();
		   Thread.sleep(2000);
		   
		   driver.findElement(By.xpath("//android.widget.TextView[contains(@resource-id,'android:id/title') and @text='APN']")).click();
		   Thread.sleep(2000);
		   
		   driver.findElement(By.xpath("//android.widget.EditText[contains(@resource-id,'android:id/edit') and @index='0']")).sendKeys("123");
		   Thread.sleep(3000);
		   
		   driver.findElement(By.xpath("//android.widget.Button[contains(@resource-id,'android:id/button1') and @text='OK']")).click();
		   Thread.sleep(2000);
		   
		   driver.findElement(By.xpath("//android.widget.TextView[contains(@resource-id,'android:id/title') and @text='Username']")).click();
		   Thread.sleep(2000);
		   
		   driver.findElement(By.xpath("//android.widget.EditText[contains(@resource-id,'android:id/edit') and @index='0']")).sendKeys("newAPN");
		   Thread.sleep(3000);
		   
		   driver.findElement(By.xpath("//android.widget.Button[contains(@resource-id,'android:id/button1') and @text='OK']")).click();
		   Thread.sleep(2000);
		   
		   driver.findElement(By.xpath("//android.widget.TextView[contains(@resource-id,'android:id/title') and @text='Password']")).click();
		   Thread.sleep(2000);
		   
		   driver.findElement(By.xpath("//android.widget.EditText[contains(@resource-id,'android:id/edit') and @index='0']")).sendKeys("123456");
		   Thread.sleep(3000);
		   
		   driver.findElement(By.xpath("//android.widget.Button[contains(@resource-id,'android:id/button1') and @text='OK']")).click();
		   Thread.sleep(2000);
		   /*
		   this.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK));

		   driver.findElement(By.xpath("//android.widget.Button[contains(@resource-id,'android:id/button2') and @text='Exit']")).click();
		   Thread.sleep(2000);*/
		  
		/*   int keyCode = 0;
		KeyEvent event = null;
		this.onKeyDown(keyCode, event); */
		   driver.pressKeyCode(AndroidKeyCode.BACK);
		   Thread.sleep(3000);
		   driver.findElement(By.xpath("//android.widget.Button[contains(@resource-id,'android:id/button2') and @text='Exit']")).click();
		   Thread.sleep(3000);
		   } catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.out.println(e);
			}
  }
 

}

