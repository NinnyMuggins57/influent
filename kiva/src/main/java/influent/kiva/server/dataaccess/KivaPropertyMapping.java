package influent.kiva.server.dataaccess;

import influent.idl.FL_PropertyTag;

public class KivaPropertyMapping
{
	private FL_PropertyTag[] _propertyTags;
	private String _friendlyName;
	
	public FL_PropertyTag[] getPropertyTags() {
		return _propertyTags;
	}
	
	public String getFriendlyName() {
		return _friendlyName;
	}
	
    public KivaPropertyMapping(String friendlyName, FL_PropertyTag ...fl_PropertyTags) {
    	_friendlyName = friendlyName;
    	_propertyTags = new FL_PropertyTag[fl_PropertyTags.length];
    	for (int i = 0; i < fl_PropertyTags.length; i++) {
    		_propertyTags[i] = fl_PropertyTags[i];
    	}
    }
};