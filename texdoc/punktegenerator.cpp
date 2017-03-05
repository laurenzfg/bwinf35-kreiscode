#include<iostream>
#include <iomanip> // für setprecision
#include<cmath>
using namespace std;

int main(int argc, char *argv[]) {
	cout << setprecision(10);
	
	double radToDeg = M_PI / 180;

	for (double k = 0; k <= 15; ++k) {
		double wert = k * 22.5;

		double cosinus = cos(wert * radToDeg);
		double sinus = sin(wert * radToDeg);
		for (double i = 4.5; i <= 5.5; ++i) {
			double x = cosinus*i;
			double y = sinus*i;

			cout << "\\fill[red] (" << x << "," << y << ") circle [radius=0.1]; \n";
		}
	}
}