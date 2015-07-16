//============================================================================
// Name        : test-cpp.cpp
// Author      : 
// Version     :
// Copyright   : Your copyright notice
// Description : Hello World in C++, Ansi-style
//============================================================================

#include <iostream>
using namespace std;

int main() {
	  int i, j = 0;
	  if ( (i = j) )                  // Non-compliant
	  {
	    return 0;
	  }
	  cout << "Hello World!!!" << endl; // prints Hello World!!!
	return 0;
}
