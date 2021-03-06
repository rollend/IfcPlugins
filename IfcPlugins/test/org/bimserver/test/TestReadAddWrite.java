package org.bimserver.test;

/******************************************************************************
 * Copyright (C) 2009-2018  BIMserver.org
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see {@literal<http://www.gnu.org/licenses/>}.
 *****************************************************************************/

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;

import org.apache.commons.io.IOUtils;
import org.bimserver.emf.IfcModelInterface;
import org.bimserver.emf.IfcModelInterfaceException;
import org.bimserver.emf.PackageMetaData;
import org.bimserver.emf.Schema;
import org.bimserver.ifc.step.deserializer.Ifc2x3tc1StepDeserializer;
import org.bimserver.ifc.step.serializer.Ifc2x3tc1StepSerializer;
import org.bimserver.models.ifc2x3tc1.Ifc2x3tc1Package;
import org.bimserver.models.ifc2x3tc1.IfcBuilding;
import org.bimserver.models.ifc2x3tc1.IfcElementQuantity;
import org.bimserver.models.ifc2x3tc1.IfcQuantityVolume;
import org.bimserver.models.ifc2x3tc1.IfcRelDefinesByProperties;
import org.bimserver.plugins.PluginConfiguration;
import org.bimserver.plugins.deserializers.DeserializeException;
import org.bimserver.plugins.serializers.SerializerException;
import org.junit.Test;

public class TestReadAddWrite {
	@Test
	public void test() {
		Ifc2x3tc1StepDeserializer deserializer = new Ifc2x3tc1StepDeserializer();
		PackageMetaData packageMetaData = new PackageMetaData(Ifc2x3tc1Package.eINSTANCE, Schema.IFC2X3TC1, Paths.get("tmp"));
		deserializer.init(packageMetaData);

		try {
			URL url = new URL("https://raw.githubusercontent.com/opensourceBIM/IFC-files/master/HHS%20Office/construction.ifc");
			InputStream openStream = url.openStream();
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			IOUtils.copy(openStream, baos);
			IfcModelInterface model = deserializer.read(new ByteArrayInputStream(baos.toByteArray()), "", baos.size(), null);

			// This is needed so we start with a clean slate of express id's
			model.resetExpressIds();
			
			// This is needed so we continue counting at highest already existing oid
			model.fixOidCounter();
			
			for (IfcBuilding building : model.getAllWithSubTypes(IfcBuilding.class)) {
				try {
					// Use createAndAdd to actually add the object to the model
					IfcQuantityVolume g_volume = model.createAndAdd(IfcQuantityVolume.class);
					g_volume.setName("Test Quantity");
					g_volume.setVolumeValue(1000000000);

					IfcElementQuantity el_gv = model.createAndAdd(IfcElementQuantity.class);
					el_gv.getQuantities().add(g_volume);

					IfcRelDefinesByProperties ifcRelDefinesByProperties1 = model.createAndAdd(IfcRelDefinesByProperties.class);
					ifcRelDefinesByProperties1.setRelatingPropertyDefinition(el_gv);
					ifcRelDefinesByProperties1.getRelatedObjects().add(building);
					building.getIsDefinedBy().add(ifcRelDefinesByProperties1);
				} catch (IfcModelInterfaceException e) {
					e.printStackTrace();
				}
			}
			
			Ifc2x3tc1StepSerializer serializer = new Ifc2x3tc1StepSerializer(new PluginConfiguration());
			serializer.init(model, null, true); // Put "true" as the last argument in order to generate new express id's
			serializer.writeToFile(Paths.get("output.ifc"), null);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (DeserializeException e) {
			e.printStackTrace();
		} catch (SerializerException e) {
			e.printStackTrace();
		}
	}
}
