/*
 * Copyright (c) 2020-2024 IBA Group.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   IBA Group
 *   Zowe Community
 */

package eu.ibagroup.formainframe.vfs

import eu.ibagroup.formainframe.utils.lock
import eu.ibagroup.formainframe.utils.read
import eu.ibagroup.formainframe.utils.write
import org.jgrapht.Graph
import org.jgrapht.graph.GraphDelegator
import java.util.concurrent.locks.ReadWriteLock

/** Class to describe the file system as concurrent graph */
class AsConcurrentGraph<Vertex : ReadWriteLock, Edge>(
  graph: Graph<Vertex, Edge>
) : GraphDelegator<Vertex, Edge>(graph) {

  override fun getEdge(sourceVertex: Vertex, targetVertex: Vertex): Edge? {
    return lock(sourceVertex.readLock(), targetVertex.readLock()) {
      super.getEdge(sourceVertex, targetVertex)
    }
  }

  override fun addEdge(sourceVertex: Vertex, targetVertex: Vertex): Edge {
    return lock(sourceVertex.writeLock(), targetVertex.writeLock()) {
      super.addEdge(sourceVertex, targetVertex)
    }
  }

  override fun addEdge(sourceVertex: Vertex, targetVertex: Vertex, e: Edge): Boolean {
    return lock(sourceVertex.writeLock(), targetVertex.writeLock()) {
      super.addEdge(sourceVertex, targetVertex, e)
    }
  }

  override fun addVertex(v: Vertex): Boolean {
    return v.write { super.addVertex(v) }
  }

  override fun containsEdge(e: Edge): Boolean {
    return lock(delegate.getEdgeSource(e).writeLock(), delegate.getEdgeTarget(e).writeLock()) {
      super.containsEdge(e)
    }
  }

  override fun containsEdge(sourceVertex: Vertex, targetVertex: Vertex): Boolean {
    return lock(sourceVertex.readLock(), targetVertex.readLock()) {
      super.containsEdge(sourceVertex, targetVertex)
    }
  }

  override fun containsVertex(v: Vertex): Boolean {
    return v.read { super.containsVertex(v) }
  }

  override fun degreeOf(vertex: Vertex): Int {
    return vertex.read { super.degreeOf(vertex) }
  }

  override fun edgesOf(vertex: Vertex): MutableSet<Edge> {
    return vertex.read { super.edgesOf(vertex) }
  }

  override fun inDegreeOf(vertex: Vertex): Int {
    return vertex.read { super.inDegreeOf(vertex) }
  }

  override fun incomingEdgesOf(vertex: Vertex): MutableSet<Edge> {
    return vertex.read { super.incomingEdgesOf(vertex) }
  }

  override fun outDegreeOf(vertex: Vertex): Int {
    return vertex.read { super.outDegreeOf(vertex) }
  }

  override fun outgoingEdgesOf(vertex: Vertex): MutableSet<Edge> {
    return vertex.read { super.outgoingEdgesOf(vertex) }
  }

  override fun removeEdge(e: Edge): Boolean {
    return lock(delegate.getEdgeSource(e).writeLock(), delegate.getEdgeTarget(e).writeLock()) {
      super.removeEdge(e)
    }
  }

  override fun removeEdge(sourceVertex: Vertex, targetVertex: Vertex): Edge {
    return lock(sourceVertex.writeLock(), targetVertex.writeLock()) {
      super.removeEdge(sourceVertex, targetVertex)
    }
  }

  override fun removeVertex(v: Vertex): Boolean {
    return v.write { super.removeVertex(v) }
  }

  override fun assertVertexExist(v: Vertex): Boolean {
    return v.read { super.assertVertexExist(v) }
  }

  override fun toStringFromSets(
    vertexSet: MutableCollection<out Vertex>?,
    edgeSet: MutableCollection<out Edge>?,
    directed: Boolean
  ): String {
    return super.toStringFromSets(vertexSet, edgeSet, directed)
  }

}
