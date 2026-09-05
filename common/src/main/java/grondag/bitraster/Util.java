/*
 *  Copyright 2019, 2020 grondag
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with the License.  You may obtain a copy
 *  of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 *  License for the specific language governing permissions and limitations under
 *  the License.
 */

package grondag.bitraster;

public class Util {
	static void printMask(long mask) {
		String s = "0000000000000000000000000000000000000000000000000000000000000000" + Long.toBinaryString(mask);
		s = s.substring(s.length() - 64);
		printSpaced(s.substring(0, 8));
		printSpaced(s.substring(8, 16));
		printSpaced(s.substring(16, 24));
		printSpaced(s.substring(24, 32));
		printSpaced(s.substring(32, 40));
		printSpaced(s.substring(40, 48));
		printSpaced(s.substring(48, 56));
		printSpaced(s.substring(56, 64));

		System.out.println();
	}

	public static void printSpaced(String s) {
		System.out.println(s.replace("0", "- ").replace("1", "X "));
	}
}
