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

/**
 * For crude but simple microbenchmarks - for small scope, in-game situations
 * where JMH would be more than I want.
 */
public class MicroTimer {
	private final int sampleSize;
	private final String label;
	private int hits;
	private long elapsed;
	private long min;
	private long max;
	private long started;

	private int subsetHits;
	private long subsetElapsed;
	private long subsetMin;
	private long subsetMax;

	public MicroTimer(String label, int sampleSize) {
		this.label = label;
		this.sampleSize = sampleSize;
	}

	public int hits() {
		return hits;
	}

	public long elapsed() {
		return elapsed;
	}

	public void start() {
		started = System.nanoTime();
	}

	/**
	 * Returns true if timer output stats this sample. For use if want to output
	 * supplementary information at same time.
	 */
	public boolean stop(boolean subset) {
		final long t = System.nanoTime() - started;
		elapsed += t;

		if (t < min) {
			min = t;
		}

		if (t > max) {
			max = t;
		}

		if (subset) {
			++subsetHits;
			subsetElapsed += t;

			if (t < subsetMin) {
				subsetMin = t;
			}

			if (t > subsetMax) {
				subsetMax = t;
			}
		}

		final long h = ++hits;

		if (h == sampleSize) {
			reportAndClear();
			return true;
		} else {
			return false;
		}
	}

	public void reportAndClear() {
		if (hits == 0) {
			hits = 1;
		}

		System.out.println(String.format("Avg %s duration = %,d ns, min = %d, max = %d, total duration = %,d, total runs = %,d", label,
				elapsed / hits, min, max, elapsed / 1000000, hits));

		if (subsetHits > 0) {
			if (subsetElapsed == 0) {
				subsetElapsed = 1;
			}

			System.out.println(String.format("Subset avg duration = %,d ns, min = %d, max = %d, total duration = %,d, (%d) total runs = %,d",
					subsetElapsed / subsetHits, subsetMin, subsetMax, subsetElapsed / 1000000, subsetElapsed * 100L / elapsed, subsetHits));

			subsetHits = 0;
			subsetElapsed = 0;
			subsetMax = Long.MIN_VALUE;
			subsetMin = Long.MAX_VALUE;
		}

		hits = 0;
		elapsed = 0;
		max = Long.MIN_VALUE;
		min = Long.MAX_VALUE;
	}
}
