android-fx
==========

Overview
----------------------

The Snaprkit FX module is a standalone Android library project that can be used to apply effects (or filters) and add stickers to an image. Both filters and stickers are defined externally to the library project and are loaded into the library at runtime.


Getting Started
----------------------

The easiest way to get started is by looking through the example projects in the android-fx-projects repository. This is a simple application that uses the library and defines several filters and stickers.


Filters
----------------------

The filters used within the library are specified in a single 'filter pack'. The filter pack is loaded from the application 'assets' folder at runtime. The filter pack uses a single json file (filter-pack.json) to define all of its filters. Each filter itself is contained within an asset subfolder and uses a json file (filter.json) to define the layers and assets used in the filter.

The library assumes that the first filter in the list is always the 'original' filter (name unimportant). The original filter is the filter with no effects applied.


Stickers
----------------------

The stickers used within the library are specified in a single 'sticker pack'. The sticker pack is loaded from the application 'assets' folder at runtime. The sticker pack uses a single json file (sticker-pack.json) to define all of its stickers. Stickers are defined by a single image and thumbnail. Each sticker has an id called a slug. Stickers live in the 'assets' folder in the sticker pack. Sticker thumbnails live in the 'assets/thumbs' folder with filename format: slug@2x.png


Defining Filters and Stickers
----------------------

By default, the library will look in the following locations for filters and stickers (within the application assets folder):

filter-packs/defaults
sticker-packs/defaults

To specify a different location, use the following methods on the SnaprKitFragment:

setFilterPackPath("filter_packs/example");
setStickerPackPath("sticker_packs/example");

Configuring Filters and Stickers
----------------------

Filters and stickers can be configured using two different approaches:

1. At build time, by adding a "settings" object to the Json file declaring the filters/stickers.
2. At runtime, by calling one of the static `SnaprImageEditFragmentActivity.startActivity(...)` methods and supplying a `Builder` instance with the desired settings.

Example of a "settings" declaration in `filter-pack.json`:

	{
	    "name": "Bubble",
	    "slug": "bubble",
	    "settings":{
	        "locked": true,
	        "unlock_message":"You need 5,000 PINK points to unlock this filter. Play games to earn your points!",
	        "hidden":true,
			"show_date":"TIMESTAMP",
			"hide_date":"TIMESTAMP"
	    }
	}
	
The expected format for `TIMESTAMP` is `2013-03-08 16:42:54 -0800`, which is parsed at startup of the module using the following pattern: `yyyy-MM-dd HH:mm:ss Z`. Failing to comply with this format will break the logic around when a sticker of filter may become available to the user.
Also note that:

- the `hidden` flag will override the `locked` setting. In other words, if a filter or sticker is flagged as both locked and hidden, it will simply not show up to the user.
- the `locked` flag cannot be set without an appropriate `unlock_message`.
- dates are optional.

Specifying the settings for a specific filter or sticker at runtime (i.e. if an effect item should be unlocked because the user reached a certain number of points), is done by mapping the `slug` of the item to a `SnaprSetting` instance. The latter is a simple POJO that wraps the various settings that can be made and is also used for static settings declared in Json.
It's up to the parent app to instantiate a `Map<String, SnarpSetting>` to hold the settings that should be applied at runtime. Generally, we'd recommend to use a `HashMap`, for fast lookups and to allow serialization. Internally, the app will convert other implementations to a `HashMap`, which guarantees that only one setting will ever exist for a specific filter or sticker.

Example for constructing runtime settings for a filter or sticker:

	SnaprSetting bubbleSetting = SnarpSetting.getSettings(<slug>, <locked>, <unlock_message>, <hidden>, <show_date>, <hide_date>);
	Map<String, SnaprSetting> settings = new HashMap<String, SnaprSetting>();
	settings.put(bubbleSetting.getSlug(), bubbleSetting);

The formats for the various options are identical to the Json configuration.

Once the settings have been set up, passing them on is made easy through the static `Builder` class that is required to invoke the `SnaprImageEditFragmentActivity`:

	// construct a Builder:
	Builder builder = new Builder(...);
	// add settings:
	builder.setSettings(settings);
	
	// start SnaprImageEditFragmentActivity (from another activity):
	SnaprImageEditFragmentActivity.startActivity(this, builder);
	// or start for result:
	SnaprImageEditFragmentActivity.startActivityForResult(this, builder);

Library assets
----------------------

Some assets used within the library are not loaded from assets at runtime. These assets are predefined within the application and loaded from resources. One such example is the delete button displayed when a sticker is being manipulated. The assets used to display this button are:

res/drawable-hdpi/snaprkit_btn_sticker_delete_normal.png
res/drawable-hdpi/snaprkit_btn_sticker_delete_down.png

To use application-specific assets, include an image resource with the same name in the application project.


Troubleshooting
----------------------

1. The first place to look for errors is always the logs. If the filters are not displayed, the most probable cause is an error parsing the filter json files. The log error message will indicate which filter (slug) is incorrect and where the error occurred.

2. Filters assets (images) are not fully loaded until the filter is applied. As a result, even though the filter was fully parsed, applying the effect may fail if the image doesn't exist. Check the logs.

3. Remember to declare the application assets folder as a source folder:
Project Properties > Java Build Path > Source > Add Folder
