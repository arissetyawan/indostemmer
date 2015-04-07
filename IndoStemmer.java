package org.apache.lucene.analysis;

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * 
 Porter stemmer in Java. The original paper is in
 
 Porter, 1980, An algorithm for suffix stripping, Program, Vol. 14,
 no. 3, pp 130-137,
 
 See also http://www.tartarus.org/~martin/PorterStemmer/index.html
 
 Bug 1 (reported by Gonzalo Parra 16/10/99) fixed as marked below.
 The words 'aed', 'eed', 'oed' leave k at 'a' for step 3, and b[k-1]
 is then out outside the bounds of b.
 
 Similarly,
 
 Bug 2 (reported by Steve Dyrdahl 22/2/00) fixed as marked below.
 'ion' by itself leaves j = -1 in the test for 'ion' in step 5, and
 b[j] is then outside the bounds of b.
 
 Release 3.
 
 [ This version is derived from Release 3, modified by Brian Goetz to
 optimize for fewer object creations.  ]
 
 */
import java.io.*;

/**
 *
 * Stemmer, implementing the Porter Stemming Algorithm
 *
 * The Stemmer class transforms a word into its root form.  The input
 * word can be provided a character at time (by calling add()), or at once
 * by calling one of the various stem(something) methods.
 */

class Indo
{
  private char[] b;
  private int i,    /* offset into b */
    j,    /* posisi array huruf yang dianggap terakhir di kata */
    awal = 0,    /* posisi array huruf yang dianggap pertama di kata */
    k, 
    awal0 = 0,
    k0,
    vok = 0, //vokal
    kon = 0, //konsonan
    sukukata = 0,
    panjang = 0, //panjang akhiran
    panjang0 = 0; //panjang awalan
  private String kanani, kumunya, kahlahpun, menge, penge;
  private boolean dirty = false,
                  dash = false,
                  falsedash = false,
                  kai = false,
                  kmn = false,
                  klp = false,
                  men = false,
                  pen = false;
  private static final int INC = 50; /* unit of size whereby b is increased */
  private static final int EXTRA = 1;
  private static Indo stringtemp;
  
  public Indo() {
    b = new char[INC];
    i = 0;
  }
  
  /**
   * reset() resets the stemmer so it can stem another word.  If you invoke
   * the stemmer by calling add(char) and then stem(), you must call reset()
   * before starting another word.
   */
  public void reset() { i = 0; j = 0; k = 0; awal = 0; awal0 = 0; vok = 0; panjang = 0; panjang0 = 0;
      dirty = false; dash = false; kai = false; kmn = false; klp = false; men = false; pen = false; 
      kanani = ""; kumunya = ""; kahlahpun = ""; menge = ""; penge = ""; falsedash = false; sukukata = 0;
  }
    
  public void reset(String s) {
    char[] c = b;
    int j1 = j;
    int k1 = k;
    int awal1 = awal;
    i = 0; j = 0; k = 0; awal = 0; awal0 = 0;
    for (int o = awal1; o < k1+1; o++)
        add(c[o]);
    j = j1-awal1; k = k1 - awal1; 
  }
  
  /**
   * Add a character to the word being stemmed.  When you are finished
   * adding characters, you can call stem(void) to process the word.
   */
  public void add(char ch) {
    if (b.length <= i + EXTRA) {
      char[] new_b = new char[b.length+INC];
      System.arraycopy(b, 0, new_b, 0, b.length);
      b = new_b;
    }
    b[i++] = ch;
  }
  
  /**
   * After a word has been stemmed, it can be retrieved by toString(),
   * or a reference to the internal buffer can be retrieved by getResultBuffer
   * and getResultLength (which is generally more efficient.)
   */   
  public String toString() { 
    if (awal > 0) ;
//    System.out.println (b[0]+""+b[1]+""+b[2]+""+b[3]+""+b[4]+""+b[5]);
    return new String(b,awal,i-awal); 
  }
  
  /**
   * Returns the length of the word resulting from the stemming process.
   */
  public int getResultLength() { 
    if (awal > 0);
    return i-awal; 
  }
  
  /**
   * Returns a reference to a character buffer containing the results of
   * the stemming process.  You also need to consult getResultLength()
   * to determine the length of the result.
   */
  public char[] getResultBuffer() { return b; }
  
  /* cons(i) is true <=> b[i] is a consonant. */
  private final boolean cons(int i) {
    switch (b[i]) {
      case 'a': case 'e': case 'i': case 'o': case 'u':
        return false;
      case 'y':
        return (i==k0) ? true : !cons(i-1);
      default:
        return true;
    }
  }
  
  /* vokal(i) is true <=> b[i] is a vocal. */
  private final boolean vokal(char c) {
    switch (c) {
      case 'a': case 'e': case 'i': case 'o': case 'u':
        return true;
      default:
        return false;
    }
  }
  
  /* m() measures the number of consonant sequences between k0 and j. if c is
   a consonant sequence and v a vowel sequence, and <..> indicates arbitrary
   presence,
   
   <c><v>       gives 0
   <c>v<v>      gives 0
   <c>vc<v>     gives 1
   <c>vcvc<v>   gives 2
   <c>vcvcvc<v> gives 3
   ....
   */
//jumlah suku kata
  public final int m() {
    int n = 0;
    int i = k0;
    while(true) {
      if (i > k)
        return vok;
      if (! cons(i))//kalau vokal
        break;
      i++;
    }
    vok++;
    i++;
    while(true) {
      while(true) {
        if (i > k)
          return vok;
        if (cons(i))//kalau konsonan
          break;
        i++;
      }
      i++;
//      n++;
      while(true) {
        if (i > k)
          return vok;
        if (! cons(i))//kalau vokal
          break;
        i++;
      }
      vok++;
      i++;
    }
  }
  
//jumlah sukukata - dilihat dari jumlah vokalnya
//perkecualian = diftong ai, oi, au 
  public int sukukata(String s) {
    int v = 0, index = 0;
    boolean isA = false;
    boolean isO = false;
    while(true) {
      if (index >= s.length())
        return v;
      if (vokal(s.charAt(index)))//kalau vokal
        break;
      index++;
    }
//     sukukata++;
    v++;
    if (s.charAt(index) == 'a') 
      isA = true;
    if (s.charAt(index) == 'o')
      isO = true;
    index++;
    while(true) {
      while(true) {
        if (index >= s.length())
          return v;
        if (!vokal(s.charAt(index)))//kalau konsonan
          break;
        v++;
        index++;
      }
      index++;
      isA = false;
      isO = false;
      while(true) {
        if (index >= s.length())
          return v;
        if (vokal(s.charAt(index)))//kalau vokal
          break;
        index++;
        isA = false;
        isO = false;
      }
      if (isA || isO) {
          if (s.charAt(index) != 'i')
//           sukukata++;
            v++;
      } else {
        if (s.charAt(index) == 'a') 
          isA = true;
        if (s.charAt(index) == 'o')
          isO = true;
//           sukukata++;
          v++;
      }
      index++;
      isA = false;
      isO = false;
    }
  }
  
  /* vowelinstem() is true <=> k0,...j contains a vowel */
  private final boolean vowelinstem() {
    int i;
    for (i = k0; i <= j; i++)
      if (! cons(i))
      return true;
    return false;
  }
  
  /* doublec(j) is true <=> j,(j-1) contain a double consonant. */
  private final boolean doublec(int j) {
    if (j < k0+1)
      return false;
    if (b[j] != b[j-1])
      return false;
    return cons(j);
  }
  
  /* cvc(i) is true <=> i-2,i-1,i has the form consonant - vowel - consonant
   and also if the second c is not w,x or y. this is used when trying to
   restore an e at the end of a short word. e.g.
   
   cav(e), lov(e), hop(e), crim(e), but
   snow, box, tray.
  */
  private final boolean cvc(int i) {
    if (i < k0+2 || !cons(i) || cons(i-1) || !cons(i-2))
      return false;
    else {
      int ch = b[i];
      if (ch == 'w' || ch == 'x' || ch == 'y') return false;
    }
    return true;
  }
  
  private final boolean ends(String s) {
    int l = s.length();
    int p = this.getResultLength();
//    int o = k-l+1;
    int o = p-l;
    if (o < k0)
      return false;
    for (int z = 0; z < l; z++)
      if (b[o+z] != s.charAt(z))
      return false;
    j = k-l;
    return true;
  }
  
  private final boolean ends(String s, int L2) {
    int l = s.length();
    int p = L2;
//    int o = k-l+1;
    int o = p-l;
    if (o < k0)
      return false;
    for (int z = 0; z < l; z++)
      if (b[o+z] != s.charAt(z))
      return false;
    j = k-l;
    return true;
  }
  
  private final boolean starts(String s) {
    int c = s.length();
//    int o = k-l+1;
    if (c-1 > i)
      return false;
    for (int z = 0; z < c; z++)
      if (b[z] != s.charAt(z))
      return false;
    awal0 = c;
    return true;
  }
  
  /* setto(s) sets (j+1),...k to the characters in the string s, readjusting k. */
  void setto(String s) {
    int l = s.length();
    int o = j+1;
    for (int i = 0; i < l; i++)
      b[o+i] = s.charAt(i);
    k = j+l;
    dirty = true;
  }
  
  /* r(s) is used further down. */ //replace  
  void r(String s) { if (m() > 0) setto(s); }
  
  private final void reduplikasi() {
    String dbugger = this.toString();
    String ak = berakhiran();
    String aw = berawalan();
    int sukuAwalan = 0;
    char em4 = aw.charAt(5);
    if (em4 == '6' || em4 == '7' || em4 == '8' || em4 == '9') sukuAwalan += 2;
    else if (aw.charAt(4) != '0') sukuAwalan++;
    int sukuAkhiran = 0;
    if (ak.charAt(4) != '0') sukuAkhiran++;
    if (ak.charAt(5) != '0') sukuAkhiran++;
    if (ak.charAt(6) != '0') sukuAkhiran++;
    int pAkhiran = panjang;
    int pAwalan = panjang0;
    int sk = m();
    int sukuPertama = 0;
    int sukuKedua = 0;
    boolean tekateki = false;
    
    if (sk > 1) {
      Indo pertama = new Indo();
      Indo kedua = new Indo();
      for (int u = 0; u < i; u++) {
        if (b[u] == '-' && !dash)
          dash = true;
        else if (!dash)
          pertama.add (b[u]);
        else
          kedua.add (b[u]);
      }
      String per1 = pertama.toString();
      String ke2 = kedua.toString();
      sukuKedua = sukukata(ke2);
      sukuPertama = sukukata(per1);
 
      if (dash) {
          if (per1.equalsIgnoreCase ("ke") ) { //i.e. ke-Allahan
          starts(per1);
          awal = awal0+1;
        } else if (per1.equalsIgnoreCase (ke2) ) { //i.e. ahli-ahli
          ends(ke2);
          k = j-1;//potong yang kedua
        } else if (pertama.ends (ke2) ) { //i.e. berabad-abad
          starts(per1);
          awal = awal0+1;//potong yang pertama
        } else if (kedua.ends (per1) ) { //i.e. ambil-mengambil
          ends(ke2);
          k = j-1;//potong yang kedua
        } else if (pertama.starts (ke2) ) { //i.e. ..nya-.. tidak ada kecuali typo
          System.out.println (this.toString() + " SALAH");
          starts (per1);
          awal = awal0+1;//potong yang pertama
        } else if (kedua.starts (per1) ) { //i.e. ahli-ahlimu
          ends(ke2);
          k = j-1;//potong yang kedua
        } else if (panjang0 > 0 & panjang > 0 && sukuKedua > 2){ //i.e. dibagi-bagikan
//          if (panjang > 0 && sukukata(ke2) > 2) { //kalau jumlah suku kata (termasuk akhiran) > 2
//             if ( sukuPertama - sukuAkhiran < 2) minOne(ak);
        int sukuTotal = sukuKedua - sukuAkhiran;//=0
            int awal1 = awal;
              if (penge == "peny" || menge == "meny") { 
                }
              else if ((penge == "peng" || menge == "meng") && vokal(b[4])) { //mengata-ngatai
                  if (ke2.charAt(0) == 'n' && ke2.charAt(1) == 'g') {
                      if (penge == "peng") penge = "pe";
                      else if (menge == "peng") menge = "me";
                      awal0 -= 2;
                    }
                }
              else if ((penge == "pem" || menge == "mem") && vokal(b[3])) { 
                  if (ke2.charAt(0) == 'm') {
                      if (penge == "pem") penge = "pe";
                      else if (menge == "pem") menge = "me";
                      awal0 -= 1;
                    }
                }
              else if ((penge == "pen" || menge == "men") && vokal(b[3])) { 
                  if (ke2.charAt(0) == 'n') {
                      if (penge == "pen") penge = "pe";
                      else if (menge == "pen") menge = "me";
                      awal0 -= 1;
                    }
                }
            if (sukuKedua - sukuAkhiran < 2 && sukuAwalan != 0) awal = awal0; 
            int pan = per1.length();
//         reset (this.toString());
//         dbugger = this.toString();
//         ak = berakhiran();
//         aw = berawalan();
// //             k = j;
//             int rb = kedua.getResultLength();
//             if (rb-panjang > 1)
//               rb = rb-panjang;
//             String akhiran = new String (kedua.getResultBuffer(), 0, rb);
// //             char[] new_b = new char[b.length];
// //             System.arraycopy(b, 0, new_b, awal0, pertama.getResultLength());
// System.out.println(this.toString());
            per1 = new String (b,awal0,pertama.getResultLength()-awal0);/**/
            awal = awal1;
            if (kedua.starts(per1)) { //i.e. berlain-lain
                ends(ke2);//ends(ke2);
                k = j-1;//potong yang kedua
                awal = awal0;
            } 
          } else if (falsedash) {
                k = j;
          } else {
              int aa = pertama.getResultLength();
              int ab = kedua.getResultLength();
              if (panjang > 0 && ab - panjang < aa) { 
                  minOne(ak); k = j; 
//                   System.out.println ("teka-tekiku " + this.toString()); 
                }
              if (panjang0 > 0 && aa - panjang0 < ab) { 
                  minOne(aw); awal = awal0; 
//                   System.out.println ("memaki-maki " + this.toString()); 
                }
              if (sukuAkhiran > 0 && ke2.length()-sukuAkhiran == per1.length()) {
                k = j;//potong yang kedua
            }
              if (sukuAwalan > 0 && per1.length()-sukuAwalan == ke2.length()) {
                awal = awal0;//potong yang kedua
            }
//               System.out.println("Nama diri atau kata ulang berubah bunyi: "+this.toString());
              tekateki = true;
          }
//         if (falsedash) System.out.print (this.toString() +  "   ");
        reset (this.toString());
        dbugger = this.toString();
        ak = berakhiran();
        cek(ak);
        if (!vokal(b[2]) && !vokal (b[3])) { 
            aw = "type0N"; 
            pAwalan = 0; 
        }
        else { 
            aw = berawalan(); 
            pAwalan = panjang0; 
        }
        pAkhiran = panjang;
        }
    }

    if (pAwalan > 0 && pAkhiran > 0 && !tekateki) {//pengertianmukah
        int sukuTotal = sukuPertama - sukuAwalan - sukuAkhiran;//=0
        if (klp && kahlahpun != "pun" && sukuTotal < 2) {
                if (sukuAkhiran == 3) {//anmukah
                    if (sukuAwalan == 2) minOne(aw);//peng
                    else minOne(ak);//mukah
                    sukuTotal++;//=1
                    if (sukuTotal < 2) {//=1
                        if (sukuAkhiran == 2) minOne(ak);//peng
                        else minOne(aw);//<s>peng</s>
                    }
                } else {
                    if (sukuAkhiran == 2) minOne(ak);
                    else minOne(aw);
                    sukuTotal++;
                    if (sukuTotal < 2) 
                        if (sukuAwalan == 2) minOne(aw);
                        else minOne(ak);
                }
        } else if (klp && kahlahpun == "pun" && sukuTotal < 2) {
                if (sukuAkhiran == 3) {
                    if (sukuAwalan == 2) minOne(ak);
                    else minOne(aw);
                    sukuTotal++;
                    if (sukuTotal < 2) {
                        if (sukuAwalan == 2) minOne(aw);
                        else minOne(ak);
                    }
                } else {
                    if (sukuAwalan == 2) minOne(aw);
                    else minOne(ak);
                    sukuTotal++;
                    if (sukuTotal < 2) 
                        if (sukuAkhiran == 2) minOne(ak);
                        else minOne(aw);
                }
        } else if (kmn && kumunya != "nya" && kumunya != "-nya" && sukuTotal < 2) {
                if (sukuAkhiran == 2) {
                    if (sukuAwalan == 2) minOne(aw);
                    else minOne(ak);
                    sukuTotal++;
                    if (sukuTotal < 2) {
                        if (sukuAkhiran == 2) minOne(ak);
                        else minOne(aw);
                    }
                } else {
                    minOne(aw);
                    sukuTotal++;
                    if (sukuTotal < 2) 
                        if (sukuAwalan == 2) minOne(aw);
                        else minOne(ak);
                }
        } else if (kmn && sukuTotal < 2) {
                if (sukuAkhiran == 3) {
                    if (sukuAwalan == 2) minOne(ak);
                    else minOne(aw);
                    sukuTotal++;
                    if (sukuTotal < 2) {
                        if (sukuAwalan == 2) minOne(aw);
                        else minOne(ak);
                    }
                } else {
                    if (sukuAwalan == 2) minOne(aw);
                    else minOne(ak);
                    sukuTotal++;
                    if (sukuTotal < 2) 
                        if (sukuAkhiran == 2) minOne(ak);
                        else minOne(aw);
                }
        } else if (kai && sukuTotal < 2) {
                minOne(ak);
        } 
              /*if (penge == "peny" || menge == "meny") { 
                  if(awal0 > 0) awal = awal0-1;
//                   b[3] = 's';
                }
              else if ((penge == "peng" || menge == "meng") && vokal(b[4])) { 
                  if(awal0 > 0) awal = awal0-1;
//                   b[3] = 'k';
                }
              else */if ((penge == "pem" || menge == "mem") && vokal(b[3])) { 
                  if(awal0 > 0) awal = awal0-1;
//                   b[2] = 'p';
                }
              else if ((penge == "pen" || menge == "men") && vokal(b[3])) { 
                  if(awal0 > 0) awal = awal0-1;
//                   b[2] = 't';
                }
              else if (panjang0 == 2 && !vokal(b[2]) && !vokal (b[3])) {
                }
              else {
                  if(awal0 > 0) awal = awal0;
                }
//               if ( sukuPertama - sukuAkhiran < 2) minOne(ak);
              if (k+1 != j) k = j;
          } else if (pAkhiran > 0 && pAwalan == 0 && !tekateki) {
//               if ( sukuPertama - sukuAkhiran < 2) minOne(ak);
              if (k+1 != j) k = j;
          } else if (pAwalan > 0 && pAkhiran == 0) { // && sukuPertama-sukuAkhiran < 2
              /*if (penge == "peny" || menge == "meny") { 
                  if(awal0 > 0) awal = awal0-1;
//                   b[3] = 's';
                }
              else if ((penge == "peng" || menge == "meng") && vokal(b[4])) { 
                  if(awal0 > 0) awal = awal0-1;
//                   b[3] = 'k';
                }
              else */if ((penge == "pem" || menge == "mem") && vokal(b[3])) { 
                  if(awal0 > 0) awal = awal0-1;
//                   b[2] = 'p';
                }
              else if ((penge == "pen" || menge == "men") && vokal(b[3])) { 
                  if(awal0 > 0) awal = awal0-1;
//                   b[2] = 't';
                }
              else if (panjang0 == 2 && !vokal(b[2]) && !vokal (b[3])) {
                }
              else {
                  if(awal0 > 0) awal = awal0;
                }
          }
  }
  
//untuk memotong jumlah kombinasi akhiran
  private void minOne (String s) {
      if (s.length() == 6) {
          switch(s.charAt(5)){
              case '9': awal0 -= 3; break;
              case '8': awal0 -= 3; break;
              case '7': awal0 -= 3; break;
              case '6': awal0 -= 1; break;
              case '5': awal0 -= 4; break;
              case '4': awal0 -= 4; break;
              case '3': awal0 -= 3; break;
              case '2': awal0 -= 3; break;
              case '1': awal0 -= 2; break;
              case '0': awal0 -= 3; break;
            }
//           else if (shadow.starts("penter")) { if (suku > 3) awalan = "type39"; else if (suku > 2) awalan = "type33"; else awalan = "type0N"; } 
//     else if (shadow.starts("pember")) { if (suku > 3) awalan = "type37"; else if (suku > 2) awalan = "type32"; else awalan = "type0N"; } 
//     else if (shadow.starts("penge")) { if (suku > 3) awalan = "type36"; else if (suku > 2) awalan = "type35"; else awalan = "type0N"; }  
//     else if (shadow.starts("menter")) { if (suku > 3) awalan = "type49"; else if (suku > 2) awalan = "type43"; else awalan = "type0N"; } 
//     else if (shadow.starts("memper")) { if (suku > 3) awalan = "type48"; else if (suku > 2) awalan = "type42"; else awalan = "type0N"; } 
//     else if (shadow.starts("member")) { if (suku > 3) awalan = "type47"; else if (suku > 2) awalan = "type42"; else awalan = "type0N"; } 
//     else if (shadow.starts("menge")) { if (suku > 3) awalan = "type46"; else if (suku > 2) awalan = "type45"; else awalan = "type0N"; } 
//     else if (shadow.starts("diter")) { if (suku > 3) awalan = "type59"; else if (suku > 2) awalan = "type51"; else awalan = "type0N"; } 
//     else if (shadow.starts("diper")) { if (suku > 3) awalan = "type58"; else if (suku > 2) awalan = "type51"; else awalan = "type0N"; } 
//     else if (shadow.starts("kauter")) { if (suku > 3) awalan = "type89"; else if (suku > 2) awalan = "type80"; else awalan = "type0N"; } 
//     else if (shadow.starts("kauper")) { if (suku > 3) awalan = "type88"; else if (suku > 2) awalan = "type80"; else awalan = "type0N"; } 
//     else if (shadow.starts("kuter")) { if (suku > 3) awalan = "type99"; else if (suku > 2) awalan = "type91"; else awalan = "type0N"; } 
//     else if (shadow.starts("kuper")) { if (suku > 3) awalan = "type98"; else if (suku > 2) awalan = "type91"; else awalan = "type0N"; } 
    
        } else if (s.length() == 8) {
      boolean kanani1 = true;
    switch(s.charAt(4)){
        case '1': kanani = ""; kanani1 = false; j += 1; break;
        case '2': kanani = ""; kanani1 = false; j += 2; break;
        case '3': kanani = ""; kanani1 = false; j += 3; break;
    }
    if (kanani1) {
        boolean kumunya1 = true;
    switch(s.charAt(5)){
        case '1': kumunya = ""; kumunya1 = false; j += 2; break;
        case '2': kumunya = ""; kumunya1 = false; j += 2; break;
        case '3': kumunya = ""; kumunya1 = false; j += 3; break;
        case '4': kumunya = ""; kumunya1 = false; j += 3; break;
        case '5': kumunya = ""; kumunya1 = false; j += 3; break;
        case '6': kumunya = ""; kumunya1 = false; j += 4; break;
    } if (kanani1 && kumunya1) {
    switch(s.charAt(6)){
        case '1': kahlahpun = ""; j += 3; break;
        case '2': kahlahpun = ""; j += 3; break;
        case '3': kahlahpun = ""; j += 3; break;
        case '4': kahlahpun = ""; j += 3; break;
    }
    }
    }
    if (kanani == "") kai = false;
    if (kumunya == "") kmn = false;
    if (kahlahpun == "") klp = false;
    }
  }
  
  public final String berakhiran() {
    vok = 0;//reset
    String akhiran = "type000N";
    int suku = sukukata(this.toString());
//     if (ends("wati")) { if (suku > 3) akhiran = "type6004"; else akhiran = "type000N"; }
    if (ends("wan")) { if (suku > 2) akhiran = "type5003"; else akhiran = "type000N"; }
    else if (ends("wi")) { if (suku > 2) akhiran = "type4002"; else akhiran = "type000N"; }
    
//    else if (ends("kan-nyatah")) { if (suku > 4) akhiran = "type3640"; else if (suku > 3) akhiran = "type0647"; else if (suku > 2) akhiran = "type0043"; else akhiran = "type000N"; }
    else if (ends("kan-nyapun")) { if (suku > 4) akhiran = "type3630"; else if (suku > 3) akhiran = "type0637"; else if (suku > 2) akhiran = "type0034"; else akhiran = "type000N"; }
    else if (ends("kan-nyalah")) { if (suku > 4) akhiran = "type3620"; else if (suku > 3) akhiran = "type0627"; else if (suku > 2) akhiran = "type0024"; else akhiran = "type000N"; }
    else if (ends("kan-nyakah")) { if (suku > 4) akhiran = "type3610"; else if (suku > 3) akhiran = "type0617"; else if (suku > 2) akhiran = "type0014"; else akhiran = "type000N"; }
    else if (ends("kan-nya")) { if (suku > 3) akhiran = "type3607"; else if (suku > 2) akhiran = "type0604"; else akhiran = "type000N"; }
//    else if (ends("kanmutah")) { if (suku > 4) akhiran = "type3549"; else if (suku > 3) akhiran = "type0546"; else if (suku > 2) akhiran = "type0043"; else akhiran = "type000N"; }
    else if (ends("kan-mupun")) { if (suku > 4) akhiran = "type3539"; else if (suku > 3) akhiran = "type0536"; else if (suku > 2) akhiran = "type0033"; else akhiran = "type000N"; }
    else if (ends("kan-mulah")) { if (suku > 4) akhiran = "type3529"; else if (suku > 3) akhiran = "type0526"; else if (suku > 2) akhiran = "type0023"; else akhiran = "type000N"; }
    else if (ends("kan-mukah")) { if (suku > 4) akhiran = "type3519"; else if (suku > 3) akhiran = "type0516"; else if (suku > 2) akhiran = "type0013"; else akhiran = "type000N"; }
    else if (ends("kan-mu")) { if (suku > 3) akhiran = "type3506"; else if (suku > 2) akhiran = "type0503"; else akhiran = "type000N"; }
//    else if (ends("kan-kutah")) { if (suku > 4) akhiran = "type3449"; else if (suku > 3) akhiran = "type0446"; else if (suku > 2) akhiran = "type0043"; else akhiran = "type000N"; }
    else if (ends("kan-kupun")) { if (suku > 4) akhiran = "type3439"; else if (suku > 3) akhiran = "type0436"; else if (suku > 2) akhiran = "type0033"; else akhiran = "type000N"; }
    else if (ends("kan-kulah")) { if (suku > 4) akhiran = "type3429"; else if (suku > 3) akhiran = "type0426"; else if (suku > 2) akhiran = "type0023"; else akhiran = "type000N"; }
    else if (ends("kan-kukah")) { if (suku > 4) akhiran = "type3419"; else if (suku > 3) akhiran = "type0416"; else if (suku > 2) akhiran = "type0013"; else akhiran = "type000N"; }
    else if (ends("kan-ku")) { if (suku > 3) akhiran = "type3406"; else if (suku > 2) akhiran = "type0403"; else akhiran = "type000N"; }
//    else if (ends("kannyatah")) { if (suku > 4) akhiran = "type3349"; else if (suku > 3) akhiran = "type0346"; else if (suku > 2) akhiran = "type0043"; else akhiran = "type000N"; }
    else if (ends("kannyapun")) { if (suku > 4) akhiran = "type3339"; else if (suku > 3) akhiran = "type0336"; else if (suku > 2) akhiran = "type0033"; else akhiran = "type000N"; }
    else if (ends("kannyalah")) { if (suku > 4) akhiran = "type3329"; else if (suku > 3) akhiran = "type0326"; else if (suku > 2) akhiran = "type0023"; else akhiran = "type000N"; }
    else if (ends("kannyakah")) { if (suku > 4) akhiran = "type3319"; else if (suku > 3) akhiran = "type0316"; else if (suku > 2) akhiran = "type0013"; else akhiran = "type000N"; }
    else if (ends("kannya")) { if (suku > 3) akhiran = "type3306"; else if (suku > 2) akhiran = "type0303"; else akhiran = "type000N"; }
//    else if (ends("kanmutah")) { if (suku > 4) akhiran = "type3248"; else if (suku > 3) akhiran = "type0245"; else if (suku > 2) akhiran = "type0043"; else akhiran = "type000N"; }
    else if (ends("kanmupun")) { if (suku > 4) akhiran = "type3238"; else if (suku > 3) akhiran = "type0235"; else if (suku > 2) akhiran = "type0033"; else akhiran = "type000N"; }
    else if (ends("kanmulah")) { if (suku > 4) akhiran = "type3228"; else if (suku > 3) akhiran = "type0225"; else if (suku > 2) akhiran = "type0023"; else akhiran = "type000N"; }
    else if (ends("kanmukah")) { if (suku > 4) akhiran = "type3218"; else if (suku > 3) akhiran = "type0215"; else if (suku > 2) akhiran = "type0013"; else akhiran = "type000N"; }
    else if (ends("kanmu")) { if (suku > 3) akhiran = "type3205"; else if (suku > 2) akhiran = "type0202"; else akhiran = "type000N"; }
//    else if (ends("kankutah")) { if (suku > 4) akhiran = "type3148"; else if (suku > 3) akhiran = "type0145"; else if (suku > 2) akhiran = "type0043"; else akhiran = "type000N"; }
    else if (ends("kankupun")) { if (suku > 4) akhiran = "type3138"; else if (suku > 3) akhiran = "type0135"; else if (suku > 2) akhiran = "type0033"; else akhiran = "type000N"; }
    else if (ends("kankulah")) { if (suku > 4) akhiran = "type3128"; else if (suku > 3) akhiran = "type0125"; else if (suku > 2) akhiran = "type0023"; else akhiran = "type000N"; }
    else if (ends("kankukah")) { if (suku > 4) akhiran = "type3118"; else if (suku > 3) akhiran = "type0115"; else if (suku > 2) akhiran = "type0013"; else akhiran = "type000N"; }
    else if (ends("kanku")) { if (suku > 3) akhiran = "type3105"; else if (suku > 2) akhiran = "type0102"; else akhiran = "type000N"; }
    //else if (ends("kantah")) { if (suku > 3) akhiran = "type3046"; else if (suku > 2) akhiran = "type0043"; else akhiran = "type000N"; }
    else if (ends("kanpun")) { if (suku > 3) akhiran = "type3036"; else if (suku > 2) akhiran = "type0033"; else akhiran = "type000N"; }
    else if (ends("kanlah")) { if (suku > 3) akhiran = "type3026"; else if (suku > 2) akhiran = "type0023"; else akhiran = "type000N"; }
    else if (ends("kankah")) { if (suku > 3) akhiran = "type3016"; else if (suku > 2) akhiran = "type0013"; else akhiran = "type000N"; }
    else if (ends("kan")) { if (suku > 2) akhiran = "type3003"; else akhiran = "type000N"; }
    
//    else if (ends("an-nyatah")) { if (suku > 4) akhiran = "type2640"; else if (suku > 3) akhiran = "type0647"; else if (suku > 2) akhiran = "type0043"; else akhiran = "type000N"; }
    else if (ends("an-nyapun")) { if (suku > 4) akhiran = "type2639"; else if (suku > 3) akhiran = "type0637"; else if (suku > 2) akhiran = "type0034"; else akhiran = "type000N"; }
    else if (ends("an-nyalah")) { if (suku > 4) akhiran = "type2629"; else if (suku > 3) akhiran = "type0627"; else if (suku > 2) akhiran = "type0024"; else akhiran = "type000N"; }
    else if (ends("an-nyakah")) { if (suku > 4) akhiran = "type2619"; else if (suku > 3) akhiran = "type0617"; else if (suku > 2) akhiran = "type0014"; else akhiran = "type000N"; }
    else if (ends("an-nya")) { if (suku > 3) akhiran = "type2606"; else if (suku > 2) akhiran = "type0604"; else akhiran = "type000N"; }
//    else if (ends("anmutah")) { if (suku > 4) akhiran = "type2548"; else if (suku > 3) akhiran = "type0546"; else if (suku > 2) akhiran = "type0043"; else akhiran = "type000N"; }
    else if (ends("an-mupun")) { if (suku > 4) akhiran = "type2538"; else if (suku > 3) akhiran = "type0536"; else if (suku > 2) akhiran = "type0033"; else akhiran = "type000N"; }
    else if (ends("an-mulah")) { if (suku > 4) akhiran = "type2528"; else if (suku > 3) akhiran = "type0526"; else if (suku > 2) akhiran = "type0023"; else akhiran = "type000N"; }
    else if (ends("an-mukah")) { if (suku > 4) akhiran = "type2518"; else if (suku > 3) akhiran = "type0516"; else if (suku > 2) akhiran = "type0013"; else akhiran = "type000N"; }
    else if (ends("an-mu")) { if (suku > 3) akhiran = "type2505"; else if (suku > 2) akhiran = "type0503"; else akhiran = "type000N"; }
//    else if (ends("an-kutah")) { if (suku > 4) akhiran = "type2448"; else if (suku > 3) akhiran = "type0446"; else if (suku > 2) akhiran = "type0043"; else akhiran = "type000N"; }
    else if (ends("an-kupun")) { if (suku > 4) akhiran = "type2438"; else if (suku > 3) akhiran = "type0436"; else if (suku > 2) akhiran = "type0033"; else akhiran = "type000N"; }
    else if (ends("an-kulah")) { if (suku > 4) akhiran = "type2428"; else if (suku > 3) akhiran = "type0426"; else if (suku > 2) akhiran = "type0023"; else akhiran = "type000N"; }
    else if (ends("an-kukah")) { if (suku > 4) akhiran = "type2418"; else if (suku > 3) akhiran = "type0416"; else if (suku > 2) akhiran = "type0013"; else akhiran = "type000N"; }
    else if (ends("an-ku")) { if (suku > 3) akhiran = "type2405"; else if (suku > 2) akhiran = "type0403"; else akhiran = "type000N"; }
//    else if (ends("annyatah")) { if (suku > 4) akhiran = "type2348"; else if (suku > 3) akhiran = "type0346"; else if (suku > 2) akhiran = "type0043"; else akhiran = "type000N"; }
    else if (ends("annyapun")) { if (suku > 4) akhiran = "type2338"; else if (suku > 3) akhiran = "type0336"; else if (suku > 2) akhiran = "type0033"; else akhiran = "type000N"; }
    else if (ends("annyalah")) { if (suku > 4) akhiran = "type2328"; else if (suku > 3) akhiran = "type0326"; else if (suku > 2) akhiran = "type0023"; else akhiran = "type000N"; }
    else if (ends("annyakah")) { if (suku > 4) akhiran = "type2318"; else if (suku > 3) akhiran = "type0316"; else if (suku > 2) akhiran = "type0013"; else akhiran = "type000N"; }
    else if (ends("annya")) { if (suku > 3) akhiran = "type2305"; else if (suku > 2) akhiran = "type0303"; else akhiran = "type000N"; }
//    else if (ends("anmutah")) { if (suku > 4) akhiran = "type2247"; else if (suku > 3) akhiran = "type0245"; else if (suku > 2) akhiran = "type0043"; else akhiran = "type000N"; }
    else if (ends("anmupun")) { if (suku > 4) akhiran = "type2237"; else if (suku > 3) akhiran = "type0235"; else if (suku > 2) akhiran = "type0033"; else akhiran = "type000N"; }
    else if (ends("anmulah")) { if (suku > 4) akhiran = "type2227"; else if (suku > 3) akhiran = "type0225"; else if (suku > 2) akhiran = "type0023"; else akhiran = "type000N"; }
    else if (ends("anmukah")) { if (suku > 4) akhiran = "type2217"; else if (suku > 3) akhiran = "type0215"; else if (suku > 2) akhiran = "type0013"; else akhiran = "type000N"; }
    else if (ends("anmu")) { if (suku > 3) akhiran = "type2204"; else if (suku > 2) akhiran = "type0202"; else akhiran = "type000N"; }
//    else if (ends("ankutah")) { if (suku > 4) akhiran = "type2147"; else if (suku > 3) akhiran = "type0145"; else if (suku > 2) akhiran = "type0043"; else akhiran = "type000N"; }
    else if (ends("ankupun")) { if (suku > 4) akhiran = "type2137"; else if (suku > 3) akhiran = "type0135"; else if (suku > 2) akhiran = "type0033"; else akhiran = "type000N"; }
    else if (ends("ankulah")) { if (suku > 4) akhiran = "type2127"; else if (suku > 3) akhiran = "type0125"; else if (suku > 2) akhiran = "type0023"; else akhiran = "type000N"; }
    else if (ends("ankukah")) { if (suku > 4) akhiran = "type2117"; else if (suku > 3) akhiran = "type0115"; else if (suku > 2) akhiran = "type0013"; else akhiran = "type000N"; }
    else if (ends("anku")) { if (suku > 3) akhiran = "type2104"; else if (suku > 2) akhiran = "type0102"; else akhiran = "type000N"; }
    //else if (ends("antah")) { if (suku > 3) akhiran = "type2045"; else if (suku > 2) akhiran = "type0043"; else akhiran = "type000N"; }
    else if (ends("anpun")) { if (suku > 3) akhiran = "type2035"; else if (suku > 2) akhiran = "type0033"; else akhiran = "type000N"; }
    else if (ends("anlah")) { if (suku > 3) akhiran = "type2025"; else if (suku > 2) akhiran = "type0023"; else akhiran = "type000N"; }
    else if (ends("ankah")) { if (suku > 3) akhiran = "type2015"; else if (suku > 2) akhiran = "type0013"; else akhiran = "type000N"; }
    else if (ends("an")) { if (suku > 2) akhiran = "type2002"; else akhiran = "type000N"; }
    
//    else if (ends("ai-nyatah")) { if (suku > 3) akhiran = "type0647"; else if (suku > 2) akhiran = "type0043"; else akhiran = "type000N"; }
//    else if (ends("i-nyatah")) { if (suku > 4) akhiran = "type1648"; else if (suku > 3) akhiran = "type0647"; else if (suku > 2) akhiran = "type0043"; else akhiran = "type000N"; }
    else if (ends("ai-nyapun")) { if (suku > 3) akhiran = "type0637"; else if (suku > 2) akhiran = "type0034"; else akhiran = "type000N"; }
    else if (ends("i-nyapun")) { if (suku > 4) akhiran = "type1638"; else if (suku > 3) akhiran = "type0637"; else if (suku > 2) akhiran = "type0034"; else akhiran = "type000N"; }
    else if (ends("ai-nyalah")) { if (suku > 3) akhiran = "type0627"; else if (suku > 2) akhiran = "type0024"; else akhiran = "type000N"; }
    else if (ends("i-nyalah")) { if (suku > 4) akhiran = "type1628"; else if (suku > 3) akhiran = "type0627"; else if (suku > 2) akhiran = "type0024"; else akhiran = "type000N"; }
    else if (ends("ai-nyakah")) { if (suku > 3) akhiran = "type0617"; else if (suku > 2) akhiran = "type0014"; else akhiran = "type000N"; }
    else if (ends("i-nyakah")) { if (suku > 4) akhiran = "type1618"; else if (suku > 3) akhiran = "type0617"; else if (suku > 2) akhiran = "type0014"; else akhiran = "type000N"; }
    else if (ends("ai-nya")) { if (suku > 2) akhiran = "type0604"; else akhiran = "type000N"; }
    else if (ends("i-nya")) { if (suku > 3) akhiran = "type1605"; else if (suku > 2) akhiran = "type0604"; else akhiran = "type000N"; }
//    else if (ends("aimutah")) { if (suku > 3) akhiran = "type0546"; else if (suku > 2) akhiran = "type0043"; else akhiran = "type000N"; }
//    else if (ends("imutah")) { if (suku > 4) akhiran = "type1547"; else if (suku > 3) akhiran = "type0546"; else if (suku > 2) akhiran = "type0043"; else akhiran = "type000N"; }
    else if (ends("ai-mupun")) { if (suku > 3) akhiran = "type0536"; else if (suku > 2) akhiran = "type0033"; else akhiran = "type000N"; }
    else if (ends("i-mupun")) { if (suku > 4) akhiran = "type1537"; else if (suku > 3) akhiran = "type0536"; else if (suku > 2) akhiran = "type0033"; else akhiran = "type000N"; }
    else if (ends("ai-mulah")) { if (suku > 3) akhiran = "type0526"; else if (suku > 2) akhiran = "type0023"; else akhiran = "type000N"; }
    else if (ends("i-mulah")) { if (suku > 4) akhiran = "type1527"; else if (suku > 3) akhiran = "type0526"; else if (suku > 2) akhiran = "type0023"; else akhiran = "type000N"; }
    else if (ends("ai-mukah")) { if (suku > 3) akhiran = "type0516"; else if (suku > 2) akhiran = "type0013"; else akhiran = "type000N"; }
    else if (ends("i-mukah")) { if (suku > 4) akhiran = "type1517"; else if (suku > 3) akhiran = "type0516"; else if (suku > 2) akhiran = "type0013"; else akhiran = "type000N"; }
    else if (ends("ai-mu")) { if (suku > 2) akhiran = "type0503"; else akhiran = "type000N"; }
    else if (ends("i-mu")) { if (suku > 3) akhiran = "type1504"; else if (suku > 2) akhiran = "type0503"; else akhiran = "type000N"; }
//    else if (ends("ai-kutah")) { if (suku > 3) akhiran = "type0446"; else if (suku > 2) akhiran = "type0043"; else akhiran = "type000N"; }
//    else if (ends("i-kutah")) { if (suku > 4) akhiran = "type1447"; else if (suku > 3) akhiran = "type0446"; else if (suku > 2) akhiran = "type0043"; else akhiran = "type000N"; }
    else if (ends("ai-kupun")) { if (suku > 3) akhiran = "type0436"; else if (suku > 2) akhiran = "type0033"; else akhiran = "type000N"; }
    else if (ends("i-kupun")) { if (suku > 4) akhiran = "type1437"; else if (suku > 3) akhiran = "type0436"; else if (suku > 2) akhiran = "type0033"; else akhiran = "type000N"; }
    else if (ends("ai-kulah")) { if (suku > 3) akhiran = "type0426"; else if (suku > 2) akhiran = "type0023"; else akhiran = "type000N"; }
    else if (ends("i-kulah")) { if (suku > 4) akhiran = "type1427"; else if (suku > 3) akhiran = "type0426"; else if (suku > 2) akhiran = "type0023"; else akhiran = "type000N"; }
    else if (ends("ai-kukah")) { if (suku > 3) akhiran = "type0416"; else if (suku > 2) akhiran = "type0013"; else akhiran = "type000N"; }
    else if (ends("i-kukah")) { if (suku > 4) akhiran = "type1417"; else if (suku > 3) akhiran = "type0416"; else if (suku > 2) akhiran = "type0013"; else akhiran = "type000N"; }
    else if (ends("ai-ku")) { if (suku > 2) akhiran = "type0403"; else akhiran = "type000N"; }
    else if (ends("i-ku")) { if (suku > 3) akhiran = "type1404"; else if (suku > 2) akhiran = "type0403"; else akhiran = "type000N"; }
//    else if (ends("ainyatah")) { if (suku > 3) akhiran = "type0346"; else if (suku > 2) akhiran = "type0043"; else akhiran = "type000N"; }
//    else if (ends("inyatah")) { if (suku > 4) akhiran = "type1347"; else if (suku > 3) akhiran = "type0346"; else if (suku > 2) akhiran = "type0043"; else akhiran = "type000N"; }
    else if (ends("ainyapun")) { if (suku > 3) akhiran = "type0336"; else if (suku > 2) akhiran = "type0033"; else akhiran = "type000N"; }
    else if (ends("inyapun")) { if (suku > 4) akhiran = "type1337"; else if (suku > 3) akhiran = "type0336"; else if (suku > 2) akhiran = "type0033"; else akhiran = "type000N"; }
    else if (ends("ainyalah")) { if (suku > 3) akhiran = "type0326"; else if (suku > 2) akhiran = "type0023"; else akhiran = "type000N"; }
    else if (ends("inyalah")) { if (suku > 4) akhiran = "type1327"; else if (suku > 3) akhiran = "type0326"; else if (suku > 2) akhiran = "type0023"; else akhiran = "type000N"; }
    else if (ends("ainyakah")) { if (suku > 3) akhiran = "type0316"; else if (suku > 2) akhiran = "type0013"; else akhiran = "type000N"; }
    else if (ends("inyakah")) { if (suku > 4) akhiran = "type1317"; else if (suku > 3) akhiran = "type0316"; else if (suku > 2) akhiran = "type0013"; else akhiran = "type000N"; }
    else if (ends("ainya")) { if (suku > 2) akhiran = "type0303"; else akhiran = "type000N"; }
    else if (ends("inya")) { if (suku > 3) akhiran = "type1304"; else if (suku > 2) akhiran = "type0303"; else akhiran = "type000N"; }
//    else if (ends("aimutah")) { if (suku > 3) akhiran = "type0245"; else if (suku > 2) akhiran = "type0043"; else akhiran = "type000N"; }
//    else if (ends("imutah")) { if (suku > 4) akhiran = "type1246"; else if (suku > 3) akhiran = "type0245"; else if (suku > 2) akhiran = "type0043"; else akhiran = "type000N"; }
    else if (ends("aimupun")) { if (suku > 3) akhiran = "type0235"; else if (suku > 2) akhiran = "type0033"; else akhiran = "type000N"; }
    else if (ends("imupun")) { if (suku > 4) akhiran = "type1236"; else if (suku > 3) akhiran = "type0235"; else if (suku > 2) akhiran = "type0033"; else akhiran = "type000N"; }
    else if (ends("aimulah")) { if (suku > 3) akhiran = "type0225"; else if (suku > 2) akhiran = "type0023"; else akhiran = "type000N"; }
    else if (ends("imulah")) { if (suku > 4) akhiran = "type1226"; else if (suku > 3) akhiran = "type0225"; else if (suku > 2) akhiran = "type0023"; else akhiran = "type000N"; }
    else if (ends("aimukah")) { if (suku > 3) akhiran = "type0215"; else if (suku > 2) akhiran = "type0013"; else akhiran = "type000N"; }
    else if (ends("imukah")) { if (suku > 4) akhiran = "type1216"; else if (suku > 3) akhiran = "type0215"; else if (suku > 2) akhiran = "type0013"; else akhiran = "type000N"; }
    else if (ends("aimu")) { if (suku > 2) akhiran = "type0202"; else akhiran = "type000N"; }
    else if (ends("imu")) { if (suku > 3) akhiran = "type1203"; else if (suku > 2) akhiran = "type0202"; else akhiran = "type000N"; }
//    else if (ends("aikutah")) { if (suku > 3) akhiran = "type0145"; else if (suku > 2) akhiran = "type0043"; else akhiran = "type000N"; }
//    else if (ends("ikutah")) { if (suku > 4) akhiran = "type1146"; else if (suku > 3) akhiran = "type0145"; else if (suku > 2) akhiran = "type0043"; else akhiran = "type000N"; }
    else if (ends("aikupun")) { if (suku > 3) akhiran = "type0135"; else if (suku > 2) akhiran = "type0033"; else akhiran = "type000N"; }
    else if (ends("ikupun")) { if (suku > 4) akhiran = "type1136"; else if (suku > 3) akhiran = "type0135"; else if (suku > 2) akhiran = "type0033"; else akhiran = "type000N"; }
    else if (ends("aikulah")) { if (suku > 3) akhiran = "type0125"; else if (suku > 2) akhiran = "type0023"; else akhiran = "type000N"; }
    else if (ends("ikulah")) { if (suku > 4) akhiran = "type1126"; else if (suku > 3) akhiran = "type0125"; else if (suku > 2) akhiran = "type0023"; else akhiran = "type000N"; }
    else if (ends("aikulah")) { if (suku > 3) akhiran = "type0125"; else if (suku > 2) akhiran = "type0023"; else akhiran = "type000N"; }
    else if (ends("ikulah")) { if (suku > 4) akhiran = "type1126"; else if (suku > 3) akhiran = "type0125"; else if (suku > 2) akhiran = "type0023"; else akhiran = "type000N"; }
    else if (ends("aiku")) { if (suku > 2) akhiran = "type0102"; else akhiran = "type000N"; }
    else if (ends("iku")) { if (suku > 3) akhiran = "type1103"; else if (suku > 2) akhiran = "type0102"; else akhiran = "type000N"; }
    //else if (ends("aitah")) { if (suku > 2) akhiran = "type0043"; else akhiran = "type000N"; }
    //else if (ends("itah")) { if (suku > 3) akhiran = "type1044"; else if (suku > 2) akhiran = "type0043"; else akhiran = "type000N"; }
    else if (ends("aipun")) { if (suku > 2) akhiran = "type0033"; else akhiran = "type000N"; }
    else if (ends("ipun")) { if (suku > 3) akhiran = "type1034"; else if (suku > 2) akhiran = "type0033"; else akhiran = "type000N"; }
    else if (ends("ailah")) { if (suku > 2) akhiran = "type0023"; else akhiran = "type000N"; }
    else if (ends("ilah")) { if (suku > 3) akhiran = "type1024"; else if (suku > 2) akhiran = "type0023"; else akhiran = "type000N"; }
    else if (ends("aikah")) { if (suku > 2) akhiran = "type0013"; else akhiran = "type000N"; }
    else if (ends("ikah")) { if (suku > 3) akhiran = "type1014"; else if (suku > 2) akhiran = "type0013"; else akhiran = "type000N"; }
    else if (ends("ai")) akhiran = "type000N"; //diftong
    else if (ends("i")) { if (suku > 2) akhiran = "type1001"; else akhiran = "type000N"; }

    //else if (ends("-nyatah")) { if (suku > 3) akhiran = "type0647"; else if (suku > 2) akhiran = "type0043"; else akhiran = "type000N"; }
    else if (ends("-nyapun")) { if (suku > 3) akhiran = "type0637"; else if (suku > 2) akhiran = "type0033"; else akhiran = "type000N"; }
    else if (ends("-nyalah")) { if (suku > 3) akhiran = "type0627"; else if (suku > 2) akhiran = "type0023"; else akhiran = "type000N"; }
    else if (ends("-nyakah")) { if (suku > 3) akhiran = "type0617"; else if (suku > 2) akhiran = "type0013"; else akhiran = "type000N"; }
    else if (ends("-nya")) { if (suku > 2) akhiran = "type0604"; else akhiran = "type000N"; }
    //else if (ends("-mutah")) { if (suku > 3) akhiran = "type0546"; else if (suku > 2) akhiran = "type0043"; else akhiran = "type000N"; }
    else if (ends("-mupun")) { if (suku > 3) akhiran = "type0536"; else if (suku > 2) akhiran = "type0033"; else akhiran = "type000N"; }
    else if (ends("-mulah")) { if (suku > 3) akhiran = "type0526"; else if (suku > 2) akhiran = "type0023"; else akhiran = "type000N"; }
    else if (ends("-mukah")) { if (suku > 3) akhiran = "type0516"; else if (suku > 2) akhiran = "type0013"; else akhiran = "type000N"; }
    else if (ends("-mu")) { if (suku > 2) akhiran = "type0503"; else akhiran = "type000N"; }
    //else if (ends("-kutah")) { if (suku > 3) akhiran = "type0446"; else if (suku > 2) akhiran = "type0043"; else akhiran = "type000N"; }
    else if (ends("-kupun")) { if (suku > 3) akhiran = "type0436"; else if (suku > 2) akhiran = "type0033"; else akhiran = "type000N"; }
    else if (ends("-kulah")) { if (suku > 3) akhiran = "type0426"; else if (suku > 2) akhiran = "type0023"; else akhiran = "type000N"; }
    else if (ends("-kukah")) { if (suku > 3) akhiran = "type0416"; else if (suku > 2) akhiran = "type0013"; else akhiran = "type000N"; }
    else if (ends("-ku")) { if (suku > 2) akhiran = "type0403"; else akhiran = "type000N"; }
    //else if (ends("nyatah")) { if (suku > 3) akhiran = "type0346"; else if (suku > 2) akhiran = "type0043"; else akhiran = "type000N"; }
    else if (ends("nyapun")) { if (suku > 3) akhiran = "type0336"; else if (suku > 2) akhiran = "type0033"; else akhiran = "type000N"; }
    else if (ends("nyalah")) { if (suku > 3) akhiran = "type0326"; else if (suku > 2) akhiran = "type0023"; else akhiran = "type000N"; }
    else if (ends("nyakah")) { if (suku > 3) akhiran = "type0316"; else if (suku > 2) akhiran = "type0013"; else akhiran = "type000N"; }
    else if (ends("nya")) { if (suku > 2) akhiran = "type0303"; else akhiran = "type000N"; }
    //else if (ends("mutah")) { if (suku > 3) akhiran = "type0245"; else if (suku > 2) akhiran = "type0043"; else akhiran = "type000N"; }
    else if (ends("mupun")) { if (suku > 3) akhiran = "type0235"; else if (suku > 2) akhiran = "type0033"; else akhiran = "type000N"; }
    else if (ends("mulah")) { if (suku > 3) akhiran = "type0225"; else if (suku > 2) akhiran = "type0023"; else akhiran = "type000N"; }
    else if (ends("mukah")) { if (suku > 3) akhiran = "type0215"; else if (suku > 2) akhiran = "type0013"; else akhiran = "type000N"; }
    else if (ends("mu")) { if (suku > 2) akhiran = "type0202"; else akhiran = "type000N"; }
    //else if (ends("kutah")) { if (suku > 3) akhiran = "type0145"; else if (suku > 2) akhiran = "type0013"; else akhiran = "type000N"; }
    else if (ends("kupun")) { if (suku > 3) akhiran = "type0135"; else if (suku > 2) akhiran = "type0043"; else akhiran = "type000N"; }
    else if (ends("kulah")) { if (suku > 3) akhiran = "type0125"; else if (suku > 2) akhiran = "type0033"; else akhiran = "type000N"; }
    else if (ends("kukah")) { if (suku > 3) akhiran = "type0115"; else if (suku > 2) akhiran = "type0023"; else akhiran = "type000N"; }
    else if (ends("ku")) { if (suku > 2) akhiran = "type0102"; else akhiran = "type000N"; }
    
    else if (ends("tah")) { if (suku > 2) akhiran = "type0043"; else akhiran = "type000N"; }
    else if (ends("pun")) { if (suku > 2) akhiran = "type0033"; else akhiran = "type000N"; }
    else if (ends("lah")) { if (suku > 2) akhiran = "type0023"; else akhiran = "type000N"; }  
    else if (ends("kah")) { if (suku > 2) akhiran = "type0013"; else akhiran = "type000N"; }
    
    akhiran = cek(akhiran);
    j = k-panjang;
    /* else if (ends("anilah")) akhiran = "anilah"; //kasihanilah
     else if (ends("sendirilah")) akhira = "lah"; */
    //else if (ends("aitah")) akhiran = "itah";
    //else if (ends("itah") && b[j] != 't') akhiran = "itah"; //kec. titah
    
//else if (ends("cetakannya") ||
//         ends("gerakannya") ||
//         ends("tumpukannya")) { t=false; stepNya(); }//stepKan(); }
//     j = k; 
    return akhiran;
  }
  
  private final String cek(String s) {
    String akhiran = s;
    kai = false; kmn = false; klp = false;
    switch(akhiran.charAt(4)){
        case '0': kanani = ""; break;
        case '1': kanani = "i"; break;
        case '2': kanani = "an"; break;
        case '3': kanani = "kan"; break;
        case '4': kanani = "wi"; break;
        case '5': kanani = "wan"; break;
//         case '6': kanani = "wati"; sukukata--; break;
    }
    switch(akhiran.charAt(5)){
        case '0': kumunya = ""; break;
        case '1': kumunya = "ku"; break; //kecuali: kuku (berkuku), laku (berlaku), pangku (pangku), suku (sesuku)
        case '2': kumunya = "mu"; break;
        case '3': kumunya = "nya"; break;
        case '4': kumunya = "-ku"; falsedash = true; break;
        case '5': kumunya = "-mu"; falsedash = true; break;
        case '6': kumunya = "-nya"; falsedash = true; break;
    }
    switch(akhiran.charAt(6)){
        case '0': kahlahpun = ""; break;
        case '1': kahlahpun = "kah"; break; //kecuali: langkah (melangkah), rekah (merekah)
        case '2': kahlahpun = "lah"; break; //kecuali: belah (berbelah), jumlah (berjumlah), lelah (berlelah), salah (bersalah)
        case '3': kahlahpun = "pun"; break;
        case '4': kahlahpun = "tah"; break;
    }
    switch(akhiran.charAt(7)){
        case 'N': panjang = 0; break;
        case '1': panjang = 1; break;
        case '2': panjang = 2; break;
        case '3': panjang = 3; break;
        case '4': panjang = 4; break;
        case '5': panjang = 5; break;
        case '6': panjang = 6; break;
        case '7': panjang = 7; break;
        case '8': panjang = 8; break;
        case '9': panjang = 9; break;
        case '0': panjang = 10; break;
    }
    if (kanani != "") kai = true;
    if (kumunya != "") kmn = true;
    if (kahlahpun != "") klp = true;
    return akhiran;
  }
//step2() untuk akhiran dobel LV7 (Jumlah=36) & LV6 (Jumlah=12). 
//step3() untuk akhiran dobel LV5 (Jumlah=12) & LV4 (Jumlah=12)
  private final void akhiran() {
    vok = 0;//reset
    //   if (k == k0) return; /* For Bug 1 */
    boolean t=false;
    String sufiks = berakhiran();
    if(sufiks.charAt(7) != 0) {
      ends (sufiks);
      t = true;
    }
    
    if (t==true) {
      k = j;
      if (m() > 2) {
        vok -= sukukata (sufiks);
        awalan();
      }
    } else {
      vok = 0;//reset
    }
  }
  
//step5() untuk akhiran dobel LV4 dan LV3
  public final String berawalan() {
    vok = 0;//reset
    String awalan = "type0N";
    Indo shadow = this;
    int suku = sukukata(shadow.toString());
    
    if (shadow.starts("berke")) { if (suku > 3) awalan = "type18"; else if (suku > 2) awalan = "type10"; else awalan = "type0N"; } 
    else if (shadow.starts("berse")) { if (suku > 3) awalan = "type19"; else if (suku > 2) awalan = "type10"; else awalan = "type0N"; } 
    else if (shadow.starts("ber")) { if (suku > 2) awalan = "type10"; else awalan = "type0N"; } 
//    else if (shadow.starts("be")) { if (suku > 2) awalan = "type11"; else awalan = "type0N"; } 
    else if (shadow.starts("ter")) { if (suku > 2) awalan = "type20"; else awalan = "type0N"; } 
//    else if (shadow.starts("te")) { if (suku > 2) awalan = "type21"; else awalan = "type0N"; } 
    else if (shadow.starts("penter")) { if (suku > 3) awalan = "type39"; else if (suku > 2) awalan = "type33"; else awalan = "type0N"; } 
    else if (shadow.starts("pember")) { if (suku > 3) awalan = "type37"; else if (suku > 2) awalan = "type32"; else awalan = "type0N"; } 
    else if (shadow.starts("penge")) { if (suku > 3) awalan = "type36"; else if (suku > 2) awalan = "type35"; else awalan = "type0N"; }  
    else if (shadow.starts("peng")) { if (suku > 2) awalan = "type35"; else awalan = "type0N"; } 
    else if (shadow.starts("peny")) { if (suku > 2) awalan = "type34"; else awalan = "type0N"; } 
    else if (shadow.starts("pen")) { if (suku > 2) awalan = "type33"; else awalan = "type0N"; } 
    else if (shadow.starts("pem")) { if (suku > 2) awalan = "type32"; else awalan = "type0N"; } 
    else if (shadow.starts("per")) { if (suku > 2) awalan = "type30"; else awalan = "type0N"; } 
    else if (shadow.starts("pe")) { if (suku > 2) awalan = "type31"; else awalan = "type0N"; } 
    else if (shadow.starts("menter")) { if (suku > 3) awalan = "type49"; else if (suku > 2) awalan = "type43"; else awalan = "type0N"; } 
    else if (shadow.starts("memper")) { if (suku > 3) awalan = "type48"; else if (suku > 2) awalan = "type42"; else awalan = "type0N"; } 
    else if (shadow.starts("member")) { if (suku > 3) awalan = "type47"; else if (suku > 2) awalan = "type42"; else awalan = "type0N"; } 
    else if (shadow.starts("menge")) { if (suku > 3) awalan = "type46"; else if (suku > 2) awalan = "type45"; else awalan = "type0N"; } 
    else if (shadow.starts("meng")) { if (suku > 2) awalan = "type45"; else awalan = "type0N"; } 
    else if (shadow.starts("meny")) { if (suku > 2) awalan = "type44"; else awalan = "type0N"; } 
    else if (shadow.starts("men")) { if (suku > 2) awalan = "type43"; else awalan = "type0N"; } 
    else if (shadow.starts("mem")) { if (suku > 2) awalan = "type42"; else awalan = "type0N"; } 
//    else if (shadow.starts("mer")) { if (suku > 2) awalan = "type40"; else awalan = "type0N"; } 
    else if (shadow.starts("me")) { if (suku > 2) awalan = "type41"; else awalan = "type0N"; } 
    else if (shadow.starts("diter")) { if (suku > 3) awalan = "type59"; else if (suku > 2) awalan = "type51"; else awalan = "type0N"; } 
    else if (shadow.starts("diper")) { if (suku > 3) awalan = "type58"; else if (suku > 2) awalan = "type51"; else awalan = "type0N"; } 
    else if (shadow.starts("diber")) { if (suku > 3) awalan = "type57"; else if (suku > 2) awalan = "type51"; else awalan = "type0N"; } 
    else if (shadow.starts("dike")) { if (suku > 3) awalan = "type55"; else if (suku > 2) awalan = "type51"; else awalan = "type0N"; } 
    else if (shadow.starts("di")) { if (suku > 2) awalan = "type51"; else awalan = "type0N"; } 
    else if (shadow.starts("seter")) { if (suku > 3) awalan = "type69"; else if (suku > 2) awalan = "type43"; else awalan = "type0N"; } 
    else if (shadow.starts("seper")) { if (suku > 3) awalan = "type68"; else if (suku > 2) awalan = "type42"; else awalan = "type0N"; } 
    else if (shadow.starts("seber")) { if (suku > 3) awalan = "type67"; else if (suku > 2) awalan = "type42"; else awalan = "type0N"; } 
    else if (shadow.starts("se")) { if (suku > 2) awalan = "type61"; else awalan = "type0N"; } 
    else if (shadow.starts("keter")) { if (suku > 3) awalan = "type79"; else if (suku > 2) awalan = "type43"; else awalan = "type0N"; } 
    else if (shadow.starts("keper")) { if (suku > 3) awalan = "type78"; else if (suku > 2) awalan = "type42"; else awalan = "type0N"; } 
    else if (shadow.starts("keber")) { if (suku > 3) awalan = "type77"; else if (suku > 2) awalan = "type42"; else awalan = "type0N"; } 
    else if (shadow.starts("ke-")) { if (suku > 2) awalan = "type70"; else awalan = "type0N"; } //ke-Allahan
    else if (shadow.starts("ke")) { if (suku > 2) awalan = "type71"; else awalan = "type0N"; } 
    else if (shadow.starts("kauter")) { if (suku > 3) awalan = "type89"; else if (suku > 2) awalan = "type80"; else awalan = "type0N"; } 
    else if (shadow.starts("kauper")) { if (suku > 3) awalan = "type88"; else if (suku > 2) awalan = "type80"; else awalan = "type0N"; } 
    else if (shadow.starts("kauber")) { if (suku > 3) awalan = "type87"; else if (suku > 2) awalan = "type80"; else awalan = "type0N"; } 
    else if (shadow.starts("kau")) { if (suku > 2) awalan = "type80"; else awalan = "type0N"; } 
    else if (shadow.starts("kuter")) { if (suku > 3) awalan = "type99"; else if (suku > 2) awalan = "type91"; else awalan = "type0N"; } 
    else if (shadow.starts("kuper")) { if (suku > 3) awalan = "type98"; else if (suku > 2) awalan = "type91"; else awalan = "type0N"; } 
    else if (shadow.starts("kuber")) { if (suku > 3) awalan = "type97"; else if (suku > 2) awalan = "type91"; else awalan = "type0N"; } 
    else if (shadow.starts("ku")) { if (suku > 2) awalan = "type91"; else awalan = "type0N"; } 
    
    char empat = awalan.charAt(4);
    char lima = awalan.charAt(5);
    switch(lima){
        case 'N': panjang0 = 0; awal0 = 0; break;
        case '0': panjang0 = 3; break;
        case '1': panjang0 = 2; break;
        case '2': panjang0 = 3; break;
        case '3': panjang0 = 3; break;
        case '4': panjang0 = 4; break;
        case '5': panjang0 = 4; break;
        case '6': panjang0 = 5; break;
        case '7': if (empat == '1' || empat == '5' || empat == '6' || empat == '7' || empat == '9') panjang0 = 5; 
                  else panjang0 = 6; break;
        case '8': if (empat == '1' || empat == '5' || empat == '6' || empat == '7' || empat == '9') panjang0 = 5; 
                  else panjang0 = 6; break;
        case '9': if (empat == '1' || empat == '5' || empat == '6' || empat == '7' || empat == '9') panjang0 = 5; 
                  else panjang0 = 6; break;
    }
    switch(empat){
        case 'N': awal0 = 0; break; 
        case '0': break;
        case '1': break; //ber
        case '2': break; //ter
        case '3': pen = true; 
                if (lima=='N') penge = ""; 
                else if (lima=='0') penge = "per"; 
                else if (lima=='1') penge = "pe"; 
                else if (lima=='2') penge = "pem"; 
                else if (lima=='3') penge = "pen"; 
                else if (lima=='4') penge = "peny"; 
                else if (lima=='5') penge = "peng"; 
                else if (lima=='6') penge = "penge";
                break; //per
        case '4': men = true; 
                if (lima=='N') penge = ""; 
                else if (lima=='0') menge = "mer"; 
                else if (lima=='1') menge = "me"; 
                else if (lima=='2') menge = "mem"; 
                else if (lima=='3') menge = "men"; 
                else if (lima=='4') menge = "meny"; 
                else if (lima=='5') menge = "meng"; 
                else if (lima=='6') menge = "menge";
                break; //mer
        case '5': break; //di
        case '6': break; //se
        case '7': break; //ke-/ke
        case '8': break; //kau/ku
        case '9': break;
    }
    return awalan;
//    if (k == k0) return; /* for Bug 1 */
/*    boolean t=false;
    if (starts("kauku")) t=true;
    else if (starts("ditertawa")) { t=false; stepDi(); stepTer(); }
    else if (starts("diterima")) { t=false; stepDi(); }
    else if (starts("keter")) t=true; //@@dengan beberapa perkecualian
    
    
    else if (starts("dike")) t=true; 
    //kec. kebiri, kecambah, kecele, kecewa, kecuali, kelabang, kelahi, kelabu, kelamin, kelana, kelebat
    //     keliling, keliru, kelola, kelompok, (keluar), kelupas, keluyur, kembali, kemudi, kendali, kendara
    //     kepala
    //ber+ keciak, kecibak, kecimpung, kecipak, kecumik, kelakar, keluarga
    else if (starts("dise")) t=true;
    
    //menge, me+ng.., me+ny..
    
    if (t==true && m() > 1) {
//        awal = awal0;
    } else {
      vok=0;//reset
      step6();
    }/**/
  }  
  private final void awalan() {
    vok = 0;//reset
    boolean t=false;
    String prefiks = berawalan();
    if(prefiks.length() > 0) {
      starts (prefiks);
      t = true;
    }
    
    if (t==true && m() > 1) {
//        vok -= sukukata (sufiks);
//        awalan();
      awal = awal0;
    } else {
      vok = 0;//reset
    }
  }
  
  /**
   * Stem a word provided as a String.  Returns the result as a String.
   */
  public String stem(String s) {
    if (stem(s.toCharArray(), s.length()))
      return toString();
    else
      return s;
  }
  
  /** Stem a word contained in a char[].  Returns true if the stemming process
    * resulted in a word different from the input.  You can retrieve the
    * result with getResultLength()/getResultBuffer() or toString().
    */
  public boolean stem(char[] word) {
    return stem(word, word.length);
  }
  
  /** Stem a word contained in a leading portion of a char[] array.
    * Returns true if the stemming process resulted in a word different
    * from the input.  You can retrieve the result with
    * getResultLength()/getResultBuffer() or toString().
    */
  public boolean stem(char[] word, int wordLen) {
    return stem(word, 0, wordLen);
  }
  
  /** Stem a word contained in a portion of a char[] array.  Returns
    * true if the stemming process resulted in a word different from
    * the input.  You can retrieve the result with
    * getResultLength()/getResultBuffer() or toString().
    */
  public boolean stem(char[] wordBuffer, int offset, int wordLen) {
    reset();
    if (b.length < wordLen) {
      char[] new_b = new char[wordLen + EXTRA];
      b = new_b;
    }
    System.arraycopy(wordBuffer, offset, b, 0, wordLen);
    i = wordLen;
    return stem(0);
  }
  
  /** Stem the word placed into the Stemmer buffer through calls to add().
    * Returns true if the stemming process resulted in a word different
    * from the input.  You can retrieve the result with
    * getResultLength()/getResultBuffer() or toString().
    */
  public boolean stem() {
    return stem(0);
  }
  
  public boolean stem(int i0) {
    k = i - 1;
    k0 = i0;
    if (k > k0+1) {
        berakhiran(); if (sukukata(this.toString()) == 1) System.out.print (this.toString()+ "   ");
//         berakhiran(); if (sukukata(this.toString()) == 1) System.out.println (this.toString());
//         berakhiran(); if (sukukata(this.toString()) == 3) System.out.print (this.toString()+ "   ");
//         berakhiran(); if (sukukata(this.toString()) > 3) System.out.println (this.toString());
      reduplikasi();
    } else 
    // Also, a word is considered dirty if we lopped off letters
    // Thanks to Ifigenia Vairelles for pointing this out.
    if (i != k+1)
      dirty = true;
    i = k+1;
    return dirty;
  }
  
  /** Test program for demonstrating the Stemmer.  It reads a file and
    * stems each word, writing the result to standard out.
    * Usage: Stemmer file-name
    */
  public static void main(String[] args) {
    Indo s = new Indo();
    
    try{
      BufferedReader fi = new BufferedReader(new InputStreamReader(System.in));
      System.out.print("Please enter the file name to create : ");
      String file_name = fi.readLine();
      File file = new File(file_name);
      boolean exist = file.createNewFile();
      if (!exist)//IF 1
      {
        System.out.println("File already exists.");
        System.exit(0);
      }
      else//ELSE 1
      {
        FileWriter fstream= new FileWriter(file_name);
        BufferedWriter out = new BufferedWriter(fstream);
        for (int i = 0; i < args.length; i++) {
          try {
            InputStream in = new FileInputStream(args[i]);
            byte[] buffer = new byte[1024];
            int bufferLen, offset, ch;
            
            bufferLen = in.read(buffer);
            offset = 0;
            s.reset();
            
            while(true) {
              if (offset < bufferLen)
                ch = buffer[offset++];
              else {
                bufferLen = in.read(buffer);
                offset = 0;
                if (bufferLen < 0)
                  ch = -1;
                else
                  ch = buffer[offset++];
              }
              
              //get word (separated by -)
              if (Character.isLetter((char) ch)||(char) ch == '-') {
                s.add(Character.toLowerCase((char) ch));
              }
              //stem word
              else {
                stringtemp = s;
                s.stem();
                out.write(s.toString());
                s.reset();
                if (ch < 0)
                  break;
                else {//DELETE NON-CHARACTERS SUCH AS (,),,,.,;,:,?,!,etc
                  if (ch!='('&&ch!=')')
                    out.write((char) ch);
                }
              }
            }
            
            in.close();
          }
          catch (IOException e) {
            System.out.println("error reading " + args[i]);
          }
        }
        //Close the output stream
        out.close();
      }
    }//END ELSE 1
    catch (IOException e){//Catch exception if any
      System.err.println("Error: " + e.getMessage());
    }
  }
}
