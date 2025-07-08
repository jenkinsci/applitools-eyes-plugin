import com.applitools.jenkins.ApplitoolsBuildWrapper;
import org.junit.Assert;
import org.testng.annotations.*;

public class UrlValidatorTests {

    @Test
    public void TesEmptyUrlExpectFalse()
    {
        boolean result = ApplitoolsBuildWrapper.DescriptorImpl.validURL("");
        Assert.assertFalse(result);
    }


    @Test
    public void TesNullUrlExpectFalse()
    {
        boolean result = ApplitoolsBuildWrapper.DescriptorImpl.validURL(null);
        Assert.assertFalse(result);
    }

    @Test
    public void TestInvalidUrlExpectFalse()
    {
        boolean result = ApplitoolsBuildWrapper.DescriptorImpl.validURL("https://");
        Assert.assertFalse(result);
    }

    @Test
    public void TestValidUrlExpectTrue()
    {
        boolean result = ApplitoolsBuildWrapper.DescriptorImpl.validURL("https://eyes.applitools.com");
        Assert.assertTrue(result);
    }

    @Test
    public void TestValidUrlWithQueryExpectFalse()
    {
        boolean result = ApplitoolsBuildWrapper.DescriptorImpl.validURL("https://eyes.applitools.com?a=b");
        Assert.assertFalse(result);
    }

    @Test
    public void TestInvalidUrlWithXssInjectionExpectFalse()
    {
        boolean result = ApplitoolsBuildWrapper.DescriptorImpl.validURL("https://eyes.applitools.com/\"></iframe><script>alert(1337)</script><div x=\"");
        Assert.assertFalse(result);
    }

}
