// $Id: TypeMap.java,v 1.5 2004/03/23 17:53:57 davidb Exp $
//
// Copyright (C) 2004 Verisign, Inc.

package com.verisignlabs.dnssec.security;

import java.util.*;

import org.xbill.DNS.Type;
import org.xbill.DNS.DNSOutput;

/** This class represents the multiple type maps of the NSEC
 *  record. Currently it is just used to convert the wire format type
 *  map to the int array that org.xbill.DNS.NSECRecord uses.  */

public class TypeMap
{
  private static final Integer[] integerArray = new Integer[0];

  private Set typeSet;
  
  public TypeMap()
  {
    this.typeSet = new HashSet();
  }

  /** Add the given type to the typemap. */
  public void set(int type)
  {
    typeSet.add(new Integer(type));
  }

  /** Remove the given type from the type map. */
  public void clear(int type)
  {
    typeSet.remove(new Integer(type));
  }

  /** @return true if the given type is present in the type map. */
  public boolean get(int type)
  {
    return typeSet.contains(new Integer(type));
  }
  

  public static TypeMap fromTypes(int[] types)
  {
    TypeMap m = new TypeMap();
    for (int i = 0; i < types.length; i++)
    {
      m.set(types[i]);
    }

    return m;
  }
  
  /** Given an array of bytes representing a wire-format type map,
   *  construct the TypeMap object. */
  public static TypeMap fromBytes(byte[] map)
  {
    int     m       = 0;
    TypeMap typemap = new TypeMap();
    
    int map_number;
    int byte_length;
    
    while (m < map.length)
    {
      map_number  = map[m++];
      byte_length = map[m++];
      
      for (int i = 0; i < byte_length; i++)
      {
        for (int j = 0; j < 8; j++)
        {
          if ( (map[m + i] & (1 << (7 - j))) != 0 )
          {
            typemap.set(map_number * 8 + j);
          }
        }
      }
      m += byte_length;
    }

    return typemap;
  }
  
  /** @return the normal string representation of the typemap. */
  public String toString()
  {
    int[] types = getTypes();
    Arrays.sort(types);
    
    StringBuffer sb = new StringBuffer();
    
    for (int i = 0; i < types.length; i++)
    {
      sb.append(" ");
      sb.append(Type.string(types[i]));
    }

    return sb.toString();
  }

  protected static void mapToWire(DNSOutput out, int[] types,
                                  int base, int start, int end)
  {
    // calculate the length of this map by looking at the largest
    // typecode in this section.
    int max_type = types[end - 1] & 0xFF;
    int map_length = (max_type / 8) + 1;

    // write the map "header" -- the base and the length of the map.
    out.writeU8(base & 0xFF);
    out.writeU8(map_length & 0xFF);

    // allocate a temporary scratch space for caculating the actual
    // bitmap.
    byte[] map = new byte[map_length];

    // for each type in our sub-array, set its corresponding bit in the map.
    for (int i = start; i < end; i++)
    {
      map[ (types[i] & 0xFF) / 8 ] |= ( 1 << (7 - types[i] % 8) );
    }
    // write out the resulting binary bitmap.
    for (int i = 0; i < map.length; i++)
    {
      out.writeU8(map[i]);
    }
  }
  
  public byte[] toWire()
  {
    int[] types = getTypes();

    Arrays.sort(types);

    int mapbase = -1;
    int mapstart = -1;

    DNSOutput out = new DNSOutput();
    
    for (int i = 0; i < types.length; i++)
    {
      int base = types[i] >> 8;
      if (base == mapbase) continue;
      if (mapstart >= 0)
      {
        mapToWire(out, types, mapbase, mapstart, i);
      }
      mapbase = base;
      mapstart = i;
    }
    mapToWire(out, types, mapbase, mapstart, types.length);

    return out.toByteArray();
  }
  
  public int[] getTypes()
  {
    Integer[] a = (Integer[]) typeSet.toArray(integerArray);

    int[] res = new int[a.length];
    for (int i = 0; i < res.length; i++) {
      res[i] = a[i].intValue();
    }

    return res;
  }

  public static int[] fromWireToTypes(byte[] wire_fmt)
  {
    return TypeMap.fromBytes(wire_fmt).getTypes();
  }

  public static byte[] fromTypesToWire(int[] types)
  {
    return TypeMap.fromTypes(types).toWire();
  }
    
}
