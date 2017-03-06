#include<iostream>
#include <iomanip> // für setprecision
#include<cmath>
using namespace std;

const double radToDeg = M_PI / 180;

int main(int argc, char *argv[]) {
	cout << setprecision(10);
	for (double k = 0; k <= 15; ++k) {
		double x = 0;
		double y = 0;
		for (double dazu = 0; dazu <= 1; ++dazu) {
			double wert = (k+dazu) * 22.5 * radToDeg;
			double cosinus = cos(wert);
			double sinus = sin(wert);
			for (double mal = 4.5; mal <= 5.5; ++mal) {
				x += cosinus*mal;
				y += sinus*mal;
			}
		}
		x = round(x/4.0);
		y = round(y/4.0);
		cout << "\\fill[red] (" << x << "," << y << ") circle [radius=0.1]; \n";
	}
}