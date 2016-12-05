class Power{
    public static void main(String[] a){
	System.out.println(new Pow().ComputePow(2,3));
    }
}

class Pow {

    public int ComputePow(int num1, int num2){
	int result;
	result = num1**num2;
	return result;
    }

}
