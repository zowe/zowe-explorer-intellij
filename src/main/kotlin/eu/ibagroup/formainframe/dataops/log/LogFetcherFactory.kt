package eu.ibagroup.formainframe.dataops.log

import eu.ibagroup.formainframe.dataops.DataOpsComponentFactory

/**
 * Base interface for creating MFLoggers.
 * @author Valentine Krus
 */
interface LogFetcherFactory: DataOpsComponentFactory<LogFetcher<*>>
