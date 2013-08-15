package com.macbury.fabula.terrain;

import java.util.ArrayList;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder.VertexInfo;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector3;

public class TriangleGrid {
  public static enum AttributeType {
    Position, Normal, Color, TextureCord, TilePosition
  }
  
  private static final int VERTEXT_PER_COL        = 4;
  private int rows;
  private int columns;
  private short vertexCursor;
  private short vertexIndex;
  private short indicesCursor;
  private ArrayList<GridVertex> vertexsList;
  private ArrayList<AttributeType> attributeTypes;
  
  private float[] verties;
  private short[] indices;
  private Mesh mesh;
  private int vertextCount;
  private GridVertex currentVertex;
  
  public TriangleGrid(int width, int height, boolean isStatic) {
    this.rows           = height;
    this.columns        = width;
    this.vertextCount   = rows*columns* VERTEXT_PER_COL;
    this.indices        = new short[vertextCount * 3];
    this.vertexsList    = new ArrayList<GridVertex>(rows*columns);
    this.attributeTypes = new ArrayList<AttributeType>();
  }
  
  public void using(AttributeType type) {
    if (!isUsing(type)) {
      this.attributeTypes.add(type);
    }
  }
  
  public boolean isUsing(AttributeType type) {
    return (this.attributeTypes.indexOf(type) >= 0);
  }
  
  public int getAttributesPerVertex() {
    int count = 0;
    if (isUsing(AttributeType.Position)) {
      count+=3;
    }
    
    if (isUsing(AttributeType.Normal)) {
      count+=3;
    }
    
    if (isUsing(AttributeType.TextureCord)) {
      count+=2;
    }
    
    if (isUsing(AttributeType.TilePosition)) {
      count+=2;
    }
    
    if (isUsing(AttributeType.Color)) {
      count++;
    }
    return count;
  }
  
  public void calculateNormals(int normalStart) {
    Vector3 side1  = new Vector3();
    Vector3 side2  = new Vector3();
    Vector3 normal = new Vector3();
    
    for (int i = 0; i < indices.length / 3; i++) {
      int index1 = indices[i * 3] * getAttributesPerVertex();
      int index2 = indices[i * 3 + 1] * getAttributesPerVertex();
      int index3 = indices[i * 3 + 2] * getAttributesPerVertex();
      
      
      side1.set(verties[index1 + normalStart], verties[index1 + normalStart+1], verties[index1 + normalStart+2]);
      normal = side1.crs(side2);
      //Vector3 side1 = vertices[index1].Position - vertices[index3].Position;
      ////Vector3 side2 = vertices[index1].Position - vertices[index2].Position;
      //Vector3 normal = Vector3.Cross(side1, side2);
  
      //vertices[index1].Normal += normal;
      //vertices[index2].Normal += normal;
      //vertices[index3].Normal += normal;
    }
  }
  
  public int getVertexSize() {
    return getAttributesPerVertex();
  }
  
  public void begin() {
    this.vertexCursor  = 0;
    this.indicesCursor = 0;
    this.vertexIndex   = 0;
    this.attributeTypes.clear();
    this.vertexsList.clear();
  }
  
  public short addVertex(float x, float y, float z) {
    currentVertex = new GridVertex();
    currentVertex.position.set(x, y, z);
    using(AttributeType.Position);
    this.vertexsList.add(currentVertex);
    return vertexIndex++;
  }
  
  public void addNormal() {
    this.addNormal(0.0f,0.0f,0.0f);
  }

  public void addNormal(float x, float y, float z) {
    currentVertex.normal.set(x, y, z);
    using(AttributeType.Normal);
  }

  public void addTilePos(float x, float z) {
    using(AttributeType.TilePosition);
    currentVertex.tilePosition.set(x, z);
  }
  
  public void addColorToVertex(int r, int g, int b, int a) {
    using(AttributeType.Color);
    currentVertex.color.set(r, g, b, a);
  }
  
  public void addUVMap(float u, float v) {
    using(AttributeType.TextureCord);
    currentVertex.textureCordinates.set(u, v);
  }
  
  public void addRectangle(float x, float y, float z, float width, float height) {
    short n1 = this.addVertex(x, y, z); // top left corner
    short n2 = this.addVertex(x, y, z+1f); // bottom left corner
    short n3 = this.addVertex(x+1f, y, z); // top right corner
    addIndices(n1,n2,n3);
    
    n1 = this.addVertex(x+1f, y, z+1f);
    addIndices(n3,n2,n1);
  }
  
  public void addIndices(short n1, short n2, short n3) {
    this.indices[indicesCursor++] = n1;
    this.indices[indicesCursor++] = n2;
    this.indices[indicesCursor++] = n3;
  }

  public void end() {
    this.verties       = new float[vertextCount * getAttributesPerVertex()];
    
    vertexCursor = 0;
    for (GridVertex vertex : this.vertexsList) {
      this.verties[vertexCursor++] = vertex.position.x;
      this.verties[vertexCursor++] = vertex.position.y;
      this.verties[vertexCursor++] = vertex.position.z;
      
      this.verties[vertexCursor++] = vertex.normal.x;
      this.verties[vertexCursor++] = vertex.normal.y;
      this.verties[vertexCursor++] = vertex.normal.z;
      
      this.verties[vertexCursor++] = Color.toFloatBits(vertex.color.r, vertex.color.g, vertex.color.b, vertex.color.a);
      
      this.verties[vertexCursor++] = vertex.textureCordinates.x;
      this.verties[vertexCursor++] = vertex.textureCordinates.y;
      
      this.verties[vertexCursor++] = vertex.tilePosition.x;
      this.verties[vertexCursor++] = vertex.tilePosition.y;
    }
    
    this.mesh = new Mesh(true, this.verties.length, this.indices.length, this.getVertexAttributes());
    mesh.setVertices(this.verties);
    mesh.setIndices(this.indices);
  }

  public float[] getVerties() {
    return verties;
  }

  public short[] getIndices() {
    return indices;
  }

  public Mesh getMesh() {
    return mesh;
  }

  public VertexAttribute[] getVertexAttributes() {
    ArrayList<VertexAttribute> attributes = new ArrayList<VertexAttribute>();
    
    if (isUsing(AttributeType.Position)) {
      attributes.add(new VertexAttribute(Usage.Position, 3, ShaderProgram.POSITION_ATTRIBUTE));
    }
    
    if (isUsing(AttributeType.Normal)) {
      attributes.add(new VertexAttribute(Usage.Normal, 3, ShaderProgram.NORMAL_ATTRIBUTE));
    }
    
    if (isUsing(AttributeType.Color)) {
      attributes.add(new VertexAttribute(Usage.ColorPacked, 4, ShaderProgram.COLOR_ATTRIBUTE));
    }
    
    if (isUsing(AttributeType.TextureCord)) {
      attributes.add(new VertexAttribute(Usage.TextureCoordinates, 2, "a_textCords"));
    }
    
    if (isUsing(AttributeType.TilePosition)) {
      attributes.add(new VertexAttribute(Usage.Generic, 2, "a_tile_position"));
    }
    
    return attributes.toArray(new VertexAttribute[attributes.size()]);
  }

  
  
}
