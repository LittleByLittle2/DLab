/***************************************************************************

Copyright (c) 2016, EPAM SYSTEMS INC

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

****************************************************************************/

package com.epam.dlab.core.parser;

import com.epam.dlab.core.AdapterBase;
import com.epam.dlab.core.FilterBase;
import com.epam.dlab.core.ModuleBase;
import com.epam.dlab.core.aggregate.AggregateGranularity;
import com.epam.dlab.core.aggregate.DataAggregator;
import com.epam.dlab.exception.AdapterException;
import com.epam.dlab.exception.InitializationException;
import com.epam.dlab.exception.ParseException;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects.ToStringHelper;
import org.apache.commons.lang3.StringUtils;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

/** Abstract module of parser.<br>
 * See description of {@link ModuleBase} how to create your own parser.
 */
public abstract class ParserBase extends ModuleBase {
	
	/** Default character used for decimal sign. */
	private static final char DECIMAL_SEPARATOR_DEFAULT = '.';
	
	/** Default character used for thousands separator. */
	private static final char DECIMAL_GROUPING_SEPARATOR_DEFAULT = ' ';
	
	/** Name of key for date report data. */
	public static final String DATA_KEY_START_DATE = "ParserBase.maxStartDate";

	
	/** Mapping columns from source format to target. */
	@JsonProperty
	private String columnMapping = null;
	
	/** Where condition for filtering the source data. */
	@JsonProperty
	private String whereCondition = null;
	
	/** How to aggregate the parsed data. */
	@JsonProperty
	@NotNull
	private AggregateGranularity aggregate = AggregateGranularity.NONE;
	
	/** Character used for decimal sign of source data. */
	@JsonProperty
	@NotNull
	private char decimalSeparator = DECIMAL_SEPARATOR_DEFAULT;
	
	/** Character used for thousands separator of source data. */
	@JsonProperty
	@NotNull
	private char groupingSeparator = DECIMAL_GROUPING_SEPARATOR_DEFAULT;


	/** Adapter for reading source data. */
	@JsonIgnore
	private AdapterBase adapterIn;
	
	/** Adapter for writing converted data. */
	@JsonIgnore
	private AdapterBase adapterOut;
	
	/** Filter for source and converted data. */
	@JsonIgnore
	private FilterBase filter;


	/** Column meta information. */
	@JsonIgnore
	private ColumnMeta columnMeta;
	
	/** Condition for filtering the source data. */
	@JsonIgnore
	private ConditionEvaluate condition;
	
	/** Aggregator of billing report.*/
	@JsonIgnore
	private DataAggregator aggregator;
	
	/** Common format helper. */
	@JsonIgnore
	private CommonFormat commonFormat;

	/** Parser statistics. */
	@JsonIgnore
	private final List<ParserStatistics> statistics = new ArrayList<>();
	
	/** Current parser statistics. */
	@JsonIgnore
	private
	ParserStatistics currentStatistics = null; 
	
	
	/** Return mapping columns from source format to target. */
	public String getColumnMapping() {
		return columnMapping;
	}

	/** Set mapping columns from source format to target. */
	public void setColumnMapping(String columnMapping) {
		this.columnMapping = columnMapping;
	}

	/** Return where condition for filtering the source data. */
	public String getWhereCondition() {
		return whereCondition;
	}

	/** Set where condition for filtering the source data. */
	public void setWhereCondition(String whereCondition) {
		this.whereCondition = whereCondition;
	}

	/** Return how to aggregate the parsed data. */
	public AggregateGranularity getAggregate() {
		return aggregate;
	}

	/** Set how to aggregate the parsed data.
	 * @throws InitializationException is being thrown
	 * */
	public void setAggregate(String aggregate) throws InitializationException {
		if (aggregate == null) {
			throw new InitializationException("Property aggregate cannot be null");
		}
		AggregateGranularity value = AggregateGranularity.of(aggregate);
        if (value == null) {
        	throw new InitializationException("Invalid value \"" + aggregate + "\" for property aggregate. " +
					"Should be one of: " + StringUtils.join(AggregateGranularity.values(), ", "));
        }
		this.aggregate = value;
	}

	/** Return character used for decimal sign of source data. */
	public char getDecimalSeparator() {
		return decimalSeparator;
	}
	
	/** Set character used for decimal sign of source data. */
	public void setDecimalSeparator(char decimalSeparator) {
		this.decimalSeparator = decimalSeparator;
	}
	
	/** Return character used for thousands separator of source data. */
	public char getGroupingSeparator() {
		return groupingSeparator;
	}
	
	/** Set character used for thousands separator of source data. */
	public void setGroupingSeparator(char groupingSeparator) {
		this.groupingSeparator = groupingSeparator;
	}
	
	
	/** Return the adapter for reading source data. */
	public AdapterBase getAdapterIn() {
		return adapterIn;
	}

	/** Return the adapter for writing converted data. */
	public AdapterBase getAdapterOut() {
		return adapterOut;
	}
	
	/** Return the filter for source and converted data. */
	public FilterBase getFilter() {
		return filter;
	}

	/** Return the column meta information. */
	public ColumnMeta getColumnMeta() {
		return columnMeta;
	}
	
	/** Return the condition for filtering the source data. */
	public ConditionEvaluate getCondition() {
		return condition;
	}
	
	/** Return the aggregator of billing report.*/
	public DataAggregator getAggregator() {
		return aggregator;
	}
	
	/** Return the common format helper. */
	CommonFormat getCommonFormat() {
		return commonFormat;
	}
	
	/** Return the parser statistics. */
	public List<ParserStatistics> getStatistics() {
		return statistics;
	}
	
	/** Add and return the new instance for statistics.
	 * @param entryName the name of new entry.
	 */
	ParserStatistics addStatistics(String entryName) {
		currentStatistics = new ParserStatistics(entryName);
		statistics.add(currentStatistics);
		return currentStatistics;
	}
	
	/** Return the current parser statistics. */
	protected ParserStatistics getCurrentStatistics() {
		return currentStatistics;
	}

	
	/** Initialize the parser.
	 * @throws InitializationException is being thrown
	 */
	public abstract void initialize()  throws InitializationException;
	
	/** Parse the source data to common format and write it to output adapter.
	 * @throws InitializationException is being thrown
	 * @throws AdapterException is being thrown
	 * @throws ParseException is being thrown
	 */
	public abstract void parse() throws InitializationException, AdapterException, ParseException;
	
	/** Build parser from given modules.
	 * @param adapterIn the adapter for reading source data.
	 * @param adapterOut the adapter for writing converted data.
	 * @param filter the filter for source and converted data. May be <b>null<b>.
	 */
	public ParserBase build(AdapterBase adapterIn, AdapterBase adapterOut, FilterBase filter) {
		this.adapterIn = adapterIn;
		this.adapterOut = adapterOut;
		if (filter != null) {
			filter.setParser(this);
		}
		this.filter = filter;
		return this;
	}
	
	
	/** Initialize ParserBase.
	 * @param header - the header of source data.
	 * @throws InitializationException is being thrown
	 * @throws AdapterException is being thrown
	 */
	protected void init(List<String> header) throws InitializationException, AdapterException {
		columnMeta = new ColumnMeta(columnMapping, header);
		if (whereCondition != null) {
			if (columnMeta.getSourceColumnNames() == null) {
				throw new InitializationException("To use the whereCondition property you must specify and have the header of source data");
			}
			condition = new ConditionEvaluate(columnMeta.getSourceColumnNames(), whereCondition);
		} else {
			condition = null;
		}
		commonFormat = new CommonFormat(columnMeta, decimalSeparator, groupingSeparator);

		if (aggregate != AggregateGranularity.NONE) {
			aggregator = new DataAggregator(aggregate);
		}

		if (getAdapterOut().isWriteHeader()) {
			getAdapterOut().writeHeader(columnMeta.getTargetColumnNames());
		}
	}
	
	
	/** Return the index of source column by column name. 
	 * @param columnName the name of column.
	 * @throws InitializationException is being thrown
	 */
	public int getSourceColumnIndexByName(String columnName) throws InitializationException {
		return ColumnMeta.getColumnIndexByName(columnName, columnMeta.getSourceColumnNames());
	}
	
	
	@Override
	public ToStringHelper toStringHelper(Object self) {
    	return super.toStringHelper(self)
    			.add("adapterIn", (adapterIn == null ? null : adapterIn.getType()))
    			.add("adapterOut", (adapterOut == null ? null : adapterOut.getType()))
    			.add("filter", (filter == null ? null : filter.getType()))
    			.add("columnMapping", columnMapping)
    			.add("whereCondition", whereCondition)
    			.add("aggregate", aggregate)
    			.add("decimalSeparator", decimalSeparator)
    			.add("groupingSeparator", groupingSeparator);
    }
}
