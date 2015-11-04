/**
 * 
 */
package com.meila.meigou.cachehelper;

/**
 * @author flong
 *
 */
public enum MeilaCacheType {
	None, Product, Seller, Comment;

	public String getPrefix() {
		String defaultPrefix = "MeilaCache_";
		switch (this) {
		case None:
			return defaultPrefix;
		case Product:
			return defaultPrefix + "Product_";
		case Seller:
			return defaultPrefix + "Seller_";
		case Comment:
			return defaultPrefix + "Comment_";
		default:
			return defaultPrefix;
		}
	}
}
