/*
 
    Copyright IBM Corp. 2010, 2016
    This file is part of Anomaly Detection Engine for Linux Logs (ADE).

    ADE is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    ADE is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with ADE.  If not, see <http://www.gnu.org/licenses/>.
 
*/
package org.openmainframe.ade.dbUtils;

public enum DriverType {
    DB2("db2"), DERBY("derby"), MY_SQL("mysql");

    public static DriverType parseDriverType(String driver) {
        driver = driver.toLowerCase();
        for (DriverType driverType : DriverType.values()) {
            for (String identifer : driverType.m_identifiers) {
                if (driver.contains(identifer)) {
                    return driverType;
                }
            }
        }
        throw new IllegalArgumentException("A known driver identifier must be contained in the driver=" + driver.toLowerCase() + ". Known driver indentifiers: \n" + prettyPrintAllDriverTypeIdentifiers());
    }

    private static String prettyPrintAllDriverTypeIdentifiers() {
        final StringBuilder sb = new StringBuilder();
        for (DriverType driverType : DriverType.values()) {
            sb.append(driverType.name() + " driver identifiers: ");
            for (String identifier : driverType.m_identifiers) {
                sb.append(identifier + ", ");
            }
            // remove trailing ', '
            sb.setLength(sb.length() - 2);

            sb.append("\n");
        }
        return sb.toString();
    }

    private final String[] m_identifiers;

    DriverType(String identifier, String... moreIdentifiers) {
        m_identifiers = new String[1 + moreIdentifiers.length];
        m_identifiers[0] = identifier.toLowerCase();
        for (int i = 0; i < moreIdentifiers.length; i++) {
            m_identifiers[i + 1] = moreIdentifiers[i].toLowerCase();
        }
    }
}