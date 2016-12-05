class Shadow {
    public static void main(String[] args) {
        System.out.println(1);
    }
}

class A {
    int x;

    public int f()      { return 0; }
    public int g(int x) { return 1; }
}

class B extends A {
    int x;
	int c;
	int d;
	
	public enum flags { 
		flag1, flag2, flag3, flag4 
	}
	
	public int cv(B b){
		return 0;
	}
	
	public int tester(){
		c = this.cv(new A());
		d = this.cv(new B());
		return 0;	
	}
	
	

    public boolean f()      { return true; }
    public int g(boolean x) { return 0; }
}
