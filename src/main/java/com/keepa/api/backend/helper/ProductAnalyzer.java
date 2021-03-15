package com.keepa.api.backend.helper;

import static com.keepa.api.backend.structs.Product.CsvType;

/**
 * Provides methods to work on the Keepa price history CSV format.
 */
public class ProductAnalyzer {

	/**
	 * finds the extreme point in the specified interval
	 *
	 * @param csv   value/price history csv
	 * @param start start of the interval (keepa time minutes), can be 0.
	 * @param end   end of the interval (keepa time minutes), can be in the future (Integer.MAX_VALUE).
	 * @param type  the type of the csv data. If the csv includes shipping costs the extreme point will be the landing price (price + shipping).
	 * @return extremePoints (time, lowest value/price, time, highest value/price) in the given interval or -1 if no extreme point was found. If the csv includes shipping costs it will be the landing price (price + shipping).
	 */
	public static int[] getExtremePointsInIntervalWithTime(int[] csv, int start, int end, CsvType type) {
		if (csv == null || start >= end || csv.length < (type.isWithShipping ? 6 : 4))
			return new int[]{-1, -1, -1, -1};

		int[] extremeValue = new int[]{-1, Integer.MAX_VALUE, -1, -1};

		int lastTime = getLastTime(csv, type);
		int firstTime = csv[0];
		if (lastTime == -1 || firstTime == -1 || firstTime > end) return new int[]{-1, -1, -1, -1};

		if (firstTime > start)
			start = firstTime;

		int loopIncrement = (type.isWithShipping ? 3 : 2);
		int adjustedIndex = type.isWithShipping ? 2 : 1;

		for (int i = 1, j = csv.length; i < j; i += loopIncrement) {
			int c = csv[i];
			int date = csv[i - 1];
			if (date >= end)
				break;

			if (c != -1) {
				if (type.isWithShipping) {
					int s = csv[i + 1];
					c += s < 0 ? 0 : s;
				}

				if (date >= start) {
					if (c < extremeValue[1]) {
						extremeValue[1] = c;
						extremeValue[0] = csv[i - 1];
					}

					if (c > extremeValue[3]) {
						extremeValue[3] = c;
						extremeValue[2] = csv[i - 1];
					}
				} else {
					boolean isValid = false;
					if (i == j - adjustedIndex) {
						isValid = true;
					} else {
						int nextDate = csv[i + adjustedIndex];
						if (nextDate >= end || (nextDate >= start))
							isValid = true;
					}

					if (isValid) {
						if (c < extremeValue[1]) {
							extremeValue[1] = c;
							extremeValue[0] = start;
						}

						if (c > extremeValue[3]) {
							extremeValue[3] = c;
							extremeValue[2] = start;
						}
					}
				}
			}
		}

		if (extremeValue[1] == Integer.MAX_VALUE) return new int[]{-1, -1, -1, -1};
		return extremeValue;
	}

	/**
	 * Get the last value/price change.
	 *
	 * @param csv  value/price history csv
	 * @param type the type of the csv data. If the csv includes shipping costs the extreme point will be the landing price (price + shipping).
	 * @return the last value/price change delta. If the csv includes shipping costs it will be the delta of the the landing prices (price + shipping).
	 */
	private static int getDeltaLast(int[] csv, CsvType type) {
		if (type.isWithShipping) {
			if (csv == null || csv.length < 6 || csv[csv.length - 1] == -1 || csv[csv.length - 5] == -1)
				return 0;

			int v = csv[csv.length - 5];
			int s = csv[csv.length - 4];
			int totalLast = v < 0 ? v : v + (s < 0 ? 0 : s);

			v = csv[csv.length - 2];
			s = csv[csv.length - 1];
			int totalCurrent = v < 0 ? v : v + (s < 0 ? 0 : s);

			return totalCurrent - totalLast;
		} else {
			if (csv == null || csv.length < 4 || csv[csv.length - 1] == -1 || csv[csv.length - 3] == -1)
				return 0;

			return csv[csv.length - 1] - csv[csv.length - 3];
		}
	}

	/**
	 * Get the last value/price.
	 *
	 * @param csv  value/price history csv
	 * @param type the type of the csv data.
	 * @return the last value/price. If the csv includes shipping costs it will be the landing price (price + shipping).
	 */
	public static int getLast(int[] csv, CsvType type) {
		if (csv == null || csv.length == 0) return -1;

		if (type.isWithShipping) {
			int s = csv[csv.length - 1];
			int v = csv[csv.length - 2];
			return v < 0 ? v : v + (s < 0 ? 0 : s);
		}

		return csv[csv.length - 1];
	}

	/**
	 * Get the time (keepa time minutes) of the last entry. This does not correspond to the last update time, but to the last time we registered a price/value change.
	 *
	 * @param csv  value/price history csv
	 * @param type the type of the csv data.
	 * @return keepa time minutes of the last entry
	 */
	public static int getLastTime(int[] csv, CsvType type) {
		return csv == null || csv.length == 0 ? -1 : csv[csv.length - (type.isWithShipping ? 3 : 2)];
	}

	/**
	 * Get the value/price at the specified time
	 *
	 * @param csv  value/price history csv
	 * @param time value/price lookup time (keepa time minutes)
	 * @param type the type of the csv data.
	 * @return the price or value of the product at the specified time. -1 if no value was found or if the product was out of stock. If the csv includes shipping costs it will be the landing price (price + shipping).
	 */
	public static int getValueAtTime(int[] csv, int time, CsvType type) {
		if (csv == null || csv.length == 0) return -1;
		int i = 0;

		int loopIncrement = (type.isWithShipping ? 3 : 2);
		for (; i < csv.length; i += loopIncrement)
			if (csv[i] > time) break;

		if (i > csv.length) return getLast(csv, type);
		if (i < loopIncrement) return -1;

		if (type.isWithShipping) {
			int v = csv[i - 2];
			int s = csv[i - 1];
			return v < 0 ? v : v + (s < 0 ? 0 : s);
		}

		return csv[i - 1];
	}

	/**
	 * Get the price and shipping cost at the specified time
	 *
	 * @param csv  price with shipping  history csv
	 * @param time price lookup time (keepa time minutes)
	 * @return int[price, shipping] - the price and shipping cost of the product at the specified time. [-1, -1] if no price was found or if the product was out of stock.
	 */
	public static int[] getPriceAndShippingAtTime(int[] csv, int time) {
		if (csv == null || csv.length == 0) return new int[]{-1, -1};
		int i = 0;

		for (; i < csv.length; i += 3) {
			if (csv[i] > time) {
				break;
			}
		}

		if (i > csv.length) return getLastPriceAndShipping(csv);
		if (i < 3) return new int[]{-1, -1};

		return new int[]{csv[i - 2], csv[i - 1]};
	}


	/**
	 * Get the last price and shipping cost.
	 *
	 * @param csv price with shipping history csv
	 * @return int[price, shipping] - the last price and shipping cost.
	 */
	public static int[] getLastPriceAndShipping(int[] csv) {
		if (csv == null || csv.length < 3) return new int[]{-1, -1};
		return new int[]{csv[csv.length - 2], csv[csv.length - 1]};
	}


	/**
	 * @param csv  value/price history csv
	 * @param time time to begin the search
	 * @param type the type of the csv data.
	 * @return the closest value/price found to the specified time. If the csv includes shipping costs it will be the landing price (price + shipping).
	 */
	public static int getClosestValueAtTime(int[] csv, int time, CsvType type) {
		if (csv == null || csv.length == 0) return -1;
		int i = 0;
		int loopIncrement = (type.isWithShipping ? 3 : 2);
		for (; i < csv.length; i += loopIncrement)
			if (csv[i] > time) break;

		if (i > csv.length) return getLast(csv, type);
		if (i < loopIncrement) {
			if (type.isWithShipping) {
				if (csv.length < 4) {
					int v = csv[2];
					int s = csv[1];
					return v < 0 ? v : v + (s < 0 ? 0 : s);
				} else
					i += 3;
			} else {
				if (csv.length < 3)
					return csv[1];
				else
					i += 2;
			}
		}

		if (type.isWithShipping) {
			if (csv[i - 2] != -1) {
				int v = csv[i - 2];
				int s = csv[i - 1];
				return v < 0 ? v : v + (s < 0 ? 0 : s);
			} else {
				for (; i < csv.length; i += loopIncrement) {
					if (csv[i - 2] != -1) break;
				}
				if (i > csv.length) return getLast(csv, type);
				if (i < 3) return -1;
				int v = csv[i - 2];
				int s = csv[i - 1];
				return v < 0 ? v : v + (s < 0 ? 0 : s);
			}
		} else {
			if (csv[i - 1] != -1)
				return csv[i - 1];
			else {
				for (; i < csv.length; i += 2) {
					if (csv[i - 1] != -1) break;
				}
				if (i > csv.length) return getLast(csv, type);
				if (i < 2) return -1;
				return csv[i - 1];
			}
		}
	}


	/**
	 * finds the lowest and highest value/price of the csv history
	 *
	 * @param csv  value/price history csv
	 * @param type the type of the csv data.
	 * @return [0] = low, [1] = high.  If the csv includes shipping costs the extreme point will be the landing price (price + shipping). [-1, -1] if insufficient data.
	 */
	public static int[] getLowestAndHighest(int[] csv, CsvType type) {
		int[] minMax = getExtremePointsInIntervalWithTime(csv, 0, Integer.MAX_VALUE, type);
		return new int[]{minMax[1], minMax[3]};
	}

	/**
	 * finds the lowest and highest value/price of the csv history including the dates of the occurrences (in keepa time minutes).
	 *
	 * @param csv  value/price history csv
	 * @param type the type of the csv data.
	 * @return [0] = low time, [1] = low, [2] = high time, [3] = high.  If the csv includes shipping costs the extreme point will be the landing price (price + shipping). [-1, -1, -1, -1] if insufficient data.
	 */
	public static int[] getLowestAndHighestWithTime(int[] csv, CsvType type) {
		return getExtremePointsInIntervalWithTime(csv, 0, Integer.MAX_VALUE, type);
	}

	/**
	 * Returns a weighted mean of the products csv history in the last X days
	 *
	 * @param csv  value/price history csv
	 * @param now  current keepa time minutes
	 * @param days number of days the weighted mean will be calculated for (e.g. 90 days, 60 days, 30 days)
	 * @param type the type of the csv data.
	 * @return the weighted mean or -1 if insufficient history csv length (less than a day). If the csv includes shipping costs it will be the wieghted mean of the landing price (price + shipping).
	 */
	public static int calcWeightedMean(int[] csv, int now, double days, CsvType type) {
		int avg = -1;

		if (csv == null || csv.length == 0)
			return avg;

		int size = csv.length;
		int loopIncrement = (type.isWithShipping ? 3 : 2);

		int duration = (csv[size - loopIncrement] - csv[0]) / 60;
		double count = 0;

		if (size < 4 || duration < 24 * 7)
			return avg;

		if (duration < 24 * days)
			days = Math.floor(duration / 24.0);

		int adjustedIndex = type.isWithShipping ? 2 : 1;

		for (int i = 1, j = size; i < j; i = i + loopIncrement) {
			int c = csv[i];
			if (c != -1) {
				if (type.isWithShipping) {
					int s = csv[i + 1];
					c += s < 0 ? 0 : s;
				}

				if (now - csv[i - 1] < days * 24 * 60) {
					if (i == 1) {
						continue;
					}

					if (avg == -1) {
						if (csv[i - loopIncrement] == -1) {
							avg = 0;
						} else {
							double tmpCount = (days * 24 * 60 - (now - csv[i - 1])) / (24 * 60.0);
							count = tmpCount;
							int price = csv[i - loopIncrement];
							if (type.isWithShipping) {
								int s = csv[i - 2];
								price += s < 0 ? 0 : s;
							}

							avg = (int) Math.floor(price * tmpCount);
						}
					}

					if (i + adjustedIndex == j) {
						if (csv[i - loopIncrement] == -1) {
							continue;
						}
						double tmpCount = ((now - csv[j - loopIncrement]) / (24.0 * 60.0));
						count += tmpCount;
						avg += c * tmpCount;
					} else {
						double tmpCount = ((csv[i + adjustedIndex] - csv[i - 1]) / (24.0 * 60.0));
						count += tmpCount;
						avg += c * tmpCount;
					}
				} else {
					if (i == j - adjustedIndex && csv[i] != -1) {
						count = 1;
						avg = c;
					}
				}
			}
		}

		if (avg != -1) {
			if (count != 0)
				avg = (int) Math.floor(avg / count);
			else
				avg = -1;
		}

		return avg;
	}

	/**
	 * Returns true if the CSV was out of stock in the given period.
	 *
	 * @param csv   value/price history csv
	 * @param start start of the interval (keepa time minutes), can be 0.
	 * @param end   end of the interval (keepa time minutes), can be in the future (Integer.MAX_VALUE).
	 * @param type  the type of the csv data.
	 * @return was out of stock in interval, null if the csv is too short to tell.
	 */
	public static Boolean getOutOfStockInInterval(int[] csv, int start, int end, CsvType type) {
		if (type.isWithShipping) {
			if (csv == null || csv.length < 6)
				return null;
		} else if (start >= end || csv == null || csv.length < 4)
			return null;

		int loopIncrement = (type.isWithShipping ? 3 : 2);
		for (int i = 0; i < csv.length; i += loopIncrement) {
			int date = csv[i];
			if (date <= start) continue;
			if (date >= end) break;
			if (csv[i + 1] == -1) return true;
		}

		return false;
	}

	/**
	 * Returns a the percentage of time in the given interval the price type was out of stock
	 *
	 * @param csv           value/price history csv
	 * @param now           current keepa time minutes
	 * @param start         start of the interval (keepa time minutes), can be 0.
	 * @param end           end of the interval (keepa time minutes), can be in the future (Integer.MAX_VALUE).
	 * @param type          the type of the csv data.
	 * @param trackingSince the product object's trackingSince value
	 * @return percentage between 0 and 100 or -1 if insufficient data. 100 = 100% out of stock in the interval.
	 */
	public static int getOutOfStockPercentageInInterval(int[] csv, int now, int start, int end, CsvType type, int trackingSince) {
		if (!type.isPrice) return -1;
		if (start >= end) return -1;
		if (csv == null || csv.length == 0)
			return -1;

		int size = csv.length;
		int loopIncrement = (type.isWithShipping ? 3 : 2);

		int lastTime = getLastTime(csv, type);
		int firstTime = csv[0];

		if (lastTime == -1 || firstTime == -1 || firstTime > end || trackingSince > end) return -1;

		long count = 0;

		if (trackingSince > start)
			start = trackingSince;

		if (end > now)
			end = now;

		int adjustedIndex = type.isWithShipping ? 2 : 1;

		for (int i = 1, j = size; i < j; i += loopIncrement) {
			int c = csv[i];
			int date = csv[i - 1];

			if (date >= end)
				break;

			if (c != -1) {
				if (date >= start) {
					if (i == 1) {
						if (i + adjustedIndex == j) {
							return 0;
						}
					}

					int nextDate;
					if (i + adjustedIndex == j) {
						nextDate = Math.min(now, end);
					} else {
						nextDate = csv[i + adjustedIndex];
						if (nextDate > end)
							nextDate = end;
					}

					long tmpCount = nextDate - date;

					count += tmpCount;
				} else {
					if (i == j - adjustedIndex) {
						return 0;
					} else {
						int nextDate = csv[i + adjustedIndex];

						if (nextDate >= end)
							return 0;

						if (nextDate >= start)
							count = nextDate - start;
					}
				}
			}
		}

		if (count > 0)
			count = 100 - (int) Math.floor((count * 100) / (double) (end - start));
		else if (count == 0) {
			count = 100;
		}

		return (int) count;
	}
}
