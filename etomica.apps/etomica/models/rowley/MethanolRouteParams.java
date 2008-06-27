package etomica.models.rowley;

/* 
 * Parameters to specify the approach route for dimer of methanol molecules.
 * Please see Rowley et al (2006) for details.
 * 
 * K.R. Schadel 2008
 */

public class MethanolRouteParams {
	
	public static double[][] setMethanolParams(int route) {
				
		double[][] params;
		
		        //   yaw              pitch       roll       theta                     phi                    liftOff
		if (route == 1) {
			params = new double [][] { { 180.00}       , { -71.93}, {   0.00}, {-180.00}                , {  35.98}                , {5.0} };
		} else if (route == 2) {
			params = new double [][] { {   0.00}       , {   0.00}, {   0.00}, {-180.00}                , {  35.98}                , {3.0} };
		} else if (route == 3) {			  
			params = new double [][] { { 180.00}       , { -71.96}, {   0.00}, {   0.00}                , { -35.98}                , {3.5} };
		} else if (route == 4) {
			params = new double [][] { { 180.00}       , {   0.00}, { 180.00}, {   0.00}                , { 2.4012, 1.6275,-0.5715}, {3.0} };
		} else if (route == 5) {
			params = new double [][] { { 180.00}       , {   0.00}, { 180.00}, {   0.00}                , {  90.00}                , {5.0} };
		} else if (route == 6) {
			params = new double [][] { { 139.4 , 0.921}, { -28.39}, { 113.11}, { 0.3667,-0.7224,-0.0013}, { 0.8757, 0.9101,-0.5864}, {3.5} };
		} else if (route == 7) {
			params = new double [][] { { 180.00}       , {   0.00}, { 180.00}, {   0.00}                , { -90.00}                , {3.0} };
		} else if (route == 8) {
			params = new double [][] { {   0.00}       , {   0.00}, {-180.00}, {   0.00}                , { -90.00}                , {3.0} };
		} else if (route == 9) {
			params = new double [][] { {   0.00}       , { -71.95}, {   0.00}, {   0.00}                , {-0.3099, 1.29  , 0.3095}, {3.8} };
		} else if (route == 10) {
			params = new double [][] { { 180.00}       , {  71.93}, { 180.00}, {-180.00}                , {-18.05}                 , {3.5} };
		} else if (route == 11) {
			params = new double [][] { {   0.00}       , { -54.02}, {   0.00}, {-180.00}                , {-0.0119,-0.1627, 0.5874}, {3.5} };
		} else if (route == 12) {
			params = new double [][] { {   0.00}       , {  17.96}, { 180.00}, {-180.00}                , {-2.4146, 2.1464, 0.5805}, {4.4} };
		} else if (route == 13) {
			params = new double [][] { { 32.408, 1.126}, {-16.127}, {-98.667}, {-0.1378, 0.5507, 0.0051}, {-1.0206, 0.9095, 0.5855}, {4.0} };
		} else if (route == 14) {
			params = new double [][] { {  90.01}       , {   0.00}, { 161.98}, { -90.00}                , {0.00003, 2.7706,-0.0000006}, {4.0} };
		} else if (route == 15) {
			params = new double [][] { { 180.00}       , { -71.93}, { 180.00}, {-180.00}                , {-0.9778, 0.0134, 0.9993}, {4.5} };
		} else if (route == 16) {
			params = new double [][] { { 180.00}       , {   0.00}, { 180.00}, {   0.00}                , {-0.4274, 0.0023, 0.9999}, {5.0} };
		} else if (route == 17) {
			params = new double [][] { {   0.00}       , {   0.00}, {   0.00}, {-180.00}                , {-0.1847,-0.6256, 0.5873}, {3.0} };
		} else if (route == 18) {
			params = new double [][] { { 180.00}       , {  54.03}, {   0.00}, {-180.00}                , {-0.4186, 0.9375, 0.587 }, {3.8} };
		} else if (route == 19) {
			params = new double [][] { { 180.00}       , {  36.10}, {   0.00}, {   0.00}                , {18.05}                  , {5.5} };
		} else {
			throw new IllegalArgumentException("Must select route from [1,19].");
		}
		
		return params;
	}

}
