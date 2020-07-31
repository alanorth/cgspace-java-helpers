/*
    DSpace Curation Tasks
    Copyright (C) 2020  Alan Orth

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

package org.cgiar.cgspace.ctasks;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.curate.AbstractCurationTask;
import org.dspace.curate.Curator;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class CountryCodeTagger extends AbstractCurationTask
{
    private int status = Curator.CURATE_UNSET;
    private String result = null;

    private static final String PLUGIN_PREFIX = "ilri";
    private static String isocodesJsonPath;
    private static String cgspaceCountriesJsonPath;
    private static String iso3166Field;
    private static String iso3166Alpha2Field;

    private List<String> results = new ArrayList<String>();

    @Override
    public int perform(DSpaceObject dso) throws IOException
    {
        // Load configuration
        isocodesJsonPath = ConfigurationManager.getProperty(PLUGIN_PREFIX, "countrycodes.iso3166.json.path");
        cgspaceCountriesJsonPath = ConfigurationManager.getProperty(PLUGIN_PREFIX, "countrycodes.cgspaceCountries.json.resource");
        iso3166Field = ConfigurationManager.getProperty(PLUGIN_PREFIX, "countrycodes.iso3166.field");
        iso3166Alpha2Field = ConfigurationManager.getProperty(PLUGIN_PREFIX, "countrycodes.iso3166-alpha2.field");

		if (dso.getType() == Constants.ITEM)
        {
            Item item = (Item)dso;
            String itemHandle = item.getHandle();

            // Always succeed?
            status = Curator.CURATE_SUCCESS;

            Metadatum[] itemCountries = item.getMetadataByMetadataString(iso3166Field);

            // skip items that don't have country metadata
            if (itemCountries.length == 0) {
                result = itemHandle + ": no countries, skipping.";
            } else {
                Gson gson = new Gson();

                BufferedReader reader = new BufferedReader(new FileReader(isocodesJsonPath));
                CountriesVocabulary isocodesCountriesJson = gson.fromJson(reader, CountriesVocabulary.class);
                reader.close();

                reader = new BufferedReader(new InputStreamReader(this.getClass().getResourceAsStream(cgspaceCountriesJsonPath)));
                CountriesVocabulary cgspaceCountriesJson = gson.fromJson(reader, CountriesVocabulary.class);
                
                System.out.println(isocodesCountriesJson.getClass());
                System.out.println(cgspaceCountriesJson.getClass());

                for (CountriesVocabulary.Country country : cgspaceCountriesJson.countries) {
                    System.out.println(country.getName());
                }

                result = itemHandle + ": " + itemCountries.length + " countries possibly need tagging";

                // check the item's country codes, if any
                Metadatum[] itemAlpha2CountryCodes = item.getMetadataByMetadataString(iso3166Alpha2Field);

                if (itemAlpha2CountryCodes.length == 0) {
                    System.out.println(itemHandle + ": Should add codes for " + itemCountries.length + " countries.");
                }
                
                /*Country kenya = new Country("Kenya", null, "Republic of Kenya", 404, "KE", "KEN");
                Country afghanistan = new Country("Afghanistan", null, "Islamic Republic of Afghanistan", 024, "AF", "AFG");

                Map<Integer, Country> countries =  new HashMap<>();
                
                countries.put(kenya.getNumeric(), kenya);
                countries.put(afghanistan.getNumeric(), afghanistan);*/
            }

            setResult(result);
            report(result);
		}

        return status;
    }
}