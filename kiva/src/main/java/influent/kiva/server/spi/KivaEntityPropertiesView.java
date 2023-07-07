package influent.kiva.server.spi;

import influent.idl.FL_Entity;
import influent.idl.FL_Property;
import influent.server.dataaccess.DataAccessException;
import influent.server.spi.impl.GenericEntityPropertiesView;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.json.JSONObject;

public class KivaEntityPropertiesView extends GenericEntityPropertiesView {
	@Override
	public JSONObject getContent(FL_Entity entity) throws DataAccessException {
		
		List<FL_Property> props = entity.getProperties();
		Collections.sort(props, new Comparator<FL_Property>() {
			public int compare(FL_Property o1, FL_Property o2) {
				return o1.getFriendlyText().toUpperCase().compareTo(o2.getFriendlyText().toUpperCase());
			}
		});

		List<String> excludedPropKeys = Arrays.asList(new String[] {"timestamp", "image_id"});
		for (int i = props.size() - 1; i >= 0; i--) {
			if (excludedPropKeys.indexOf(props.get(i).getKey()) != -1) {
				props.remove(i);
			}
		}
		entity.setProperties(props);
		
		JSONObject content = super.getContent(entity);
		
		return content;
	}
}
