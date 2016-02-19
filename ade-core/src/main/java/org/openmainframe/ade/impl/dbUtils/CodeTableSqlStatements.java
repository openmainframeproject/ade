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
package org.openmainframe.ade.impl.dbUtils;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import org.openmainframe.ade.dbUtils.ConnectionWrapper;
import org.openmainframe.ade.exceptions.AdeException;
import org.openmainframe.ade.exceptions.AdeInternalException;
import org.openmainframe.ade.impl.dataStore.SQL;

public class CodeTableSqlStatements {

    private String m_tableName;
    private String m_idColumn;
    private String m_wordColumn;

    CodeTableSqlStatements(SQL tableName, String idColumn, String wordColumn) {
        this(tableName.toString(), idColumn, wordColumn);
    }

    CodeTableSqlStatements(String tableName, String idColumn, String wordColumn) {
        m_tableName = tableName;
        m_idColumn = idColumn;
        m_wordColumn = wordColumn;
    }

    public void deleteWord(int id) throws AdeException {
        final String sql = String.format("delete from %s where %s=%d", m_tableName, m_idColumn, id);
        ConnectionWrapper.executeDmlDefaultCon(sql);
    }

    public String getWord(int id) throws AdeException {
        final WordReader wordReader = new WordReader(id);
        wordReader.executeQuery();
        if (wordReader.m_words.size() == 0) {
            return null;
        }
        if (wordReader.m_words.size() > 1) {
            throw new AdeInternalException("Multiple words to id " + id);
        }
        return wordReader.m_words.get(0);
    }

    public int getId(String word) throws AdeException {
        final WordIdReader wordIdReader = new WordIdReader(word);
        wordIdReader.executeQuery();
        if (wordIdReader.m_ids.size() > 1) {
            throw new AdeInternalException("Duplicate ids for word " + word);
        }
        if (wordIdReader.m_ids.size() == 1) {
            return wordIdReader.m_ids.get(0);
        }
        return DbDictionary.InvalidID;
    }

    public int insertWord(String word) throws AdeException {
        new WordWriter(word).execute();
        return SpecialSqlQueries.getLastKey();
    }

    public Map<Integer, String> getAllMap() throws AdeException {
        final ReadAllWords readAllWords = new ReadAllWords();
        readAllWords.executeQuery();
        return readAllWords.valueMap;
    }

    private class WordIdReader extends QueryPreparedStatementExecuter {

        private String m_word;
        private ArrayList<Integer> m_ids = new ArrayList<Integer>();

        WordIdReader(String word) {
            super(String.format("select %s from %s where %s=?",
                    m_idColumn, m_tableName, m_wordColumn));
            m_word = word;
        }

        @Override
        protected void setParameters(PreparedStatement stmt) throws SQLException,
                AdeException {
            stmt.setString(1, m_word);
        }

        @Override
        protected void handleResultSet(ResultSet rs) throws SQLException,
                AdeException {
            m_ids.add(rs.getInt(1));
        }
    }

    private class WordReader extends QueryPreparedStatementExecuter {

        private int m_id;
        private ArrayList<String> m_words = new ArrayList<String>();

        WordReader(int id) {
            super(String.format("select %s from %s where %s=?",
                    m_wordColumn, m_tableName, m_idColumn));
            m_id = id;
        }

        @Override
        protected void setParameters(PreparedStatement stmt) throws SQLException,
                AdeException {
            stmt.setInt(1, m_id);
        }

        @Override
        protected void handleResultSet(ResultSet rs) throws SQLException,
                AdeException {
            m_words.add(rs.getString(1));
        }
    }

    private class WordWriter extends DmlPreparedStatementExecuter {

        String m_word;

        WordWriter(String word) {
            super(String.format("insert into %s (%s) values (?)",
                    m_tableName, m_wordColumn));
            m_word = word;
        }

        @Override
        protected void setParameters(PreparedStatement stmt) throws SQLException,
                AdeException {
            stmt.setString(1, m_word);
        }
    }

    private class ReadAllWords extends QueryStatementExecuter {

        public Map<Integer, String> valueMap = null;

        public ReadAllWords() {
            super(String.format("select %s,%s from %s", m_idColumn, m_wordColumn, m_tableName));
            valueMap = new TreeMap<Integer, String>();
        }

        @Override
        protected void handleResultSet(ResultSet rs) throws SQLException {
            valueMap.put(rs.getInt(1), rs.getString(2));
        }
    }

}
