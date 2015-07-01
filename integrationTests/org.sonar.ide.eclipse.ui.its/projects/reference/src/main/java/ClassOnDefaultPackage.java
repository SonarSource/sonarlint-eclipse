public class ClassOnDefaultPackage {
	
	public ClassOnDefaultPackage(int i) {
		int j = i++; // 2 issues: Unused j + reuse parameter
	}
	
	private String myMethod() { // Issue: private method not used
		return "hello";
	}
}
