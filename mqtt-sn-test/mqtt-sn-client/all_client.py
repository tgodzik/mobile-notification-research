#qpy:console
#qpy:2

try:
  False = not True
except NameError:
  True = (1 == 1)
  False = not True

# Message types
ADVERTISE, SEARCHGW, GWINFO, reserved, \
CONNECT, CONNACK, \
WILLTOPICREQ, WILLTOPIC, WILLMSGREQ, WILLMSG, \
REGISTER, REGACK, \
PUBLISH, PUBACK, PUBCOMP, PUBREC, PUBREL, reserved1, \
SUBSCRIBE, SUBACK, UNSUBSCRIBE, UNSUBACK, \
PINGREQ, PINGRESP, DISCONNECT, reserved2, \
WILLTOPICUPD, WILLTOPICRESP, WILLMSGUPD, WILLMSGRESP = range(30)

packetNames = [ "ADVERTISE", "SEARCHGW", "GWINFO", "reserved", \
"CONNECT", "CONNACK", \
"WILLTOPICREQ", "WILLTOPIC", "WILLMSGREQ", "WILLMSG", \
"REGISTER", "REGACK", \
"PUBLISH", "PUBACK", "PUBCOMP", "PUBREC", "PUBREL", "reserved", \
"SUBSCRIBE", "SUBACK", "UNSUBSCRIBE", "UNSUBACK", \
"PINGREQ", "PINGRESP", "DISCONNECT", "reserved", \
"WILLTOPICUPD", "WILLTOPICRESP", "WILLMSGUPD", "WILLMSGRESP"]

TopicIdType_Names = ["NORMAL", "PREDEFINED", "SHORT_NAME"]
TOPIC_NORMAL, TOPIC_PREDEFINED, TOPIC_SHORTNAME = range(3)

def writeInt16(length):
  return chr(length / 256)+ chr(length % 256)

def readInt16(buf):
  return ord(buf[0])*256 + ord(buf[1])

def getPacket(aSocket):
  "receive the next packet"
  buf, address = aSocket.recvfrom(65535) # get the first byte fixed header
  if buf == "":
    return None
  
  length = ord(buf[0])
  if length == 1:
    if buf == "":
      return None
    length = readInt16(buf[1:])
    
  return buf, address

def MessageType(buf):
  if ord(buf[0]) == 1:
    msgtype = ord(buf[3])
  else:
    msgtype = ord(buf[1])
  return msgtype

class Flags:
  
  def __init__(self):
    self.DUP = False          # 1 bit
    self.QoS = 0              # 2 bits
    self.Retain = False       # 1 bit
    self.Will = False         # 1 bit
    self.CleanSession = True  # 1 bit
    self.TopicIdType = 0      # 2 bits
    
  def __eq__(self, flags):
    return self.DUP == flags.DUP and \
         self.QoS == flags.QoS and \
         self.Retain == flags.Retain and \
         self.Will == flags.Will and \
         self.CleanSession == flags.CleanSession and \
         self.TopicIdType == flags.TopicIdType
  
  def __ne__(self, flags):
    return not self.__eq__(flags)
  
  def __str__(self):
    "return printable representation of our data"
    return '{DUP '+str(self.DUP)+ \
           ", QoS "+str(self.QoS)+", Retain "+str(self.Retain) + \
           ", Will "+str(self.Will)+", CleanSession "+str(self.CleanSession) + \
           ", TopicIdType "+str(self.TopicIdType)+"}"
    
  def pack(self):
    "pack data into string buffer ready for transmission down socket"
    buffer = chr( (self.DUP << 7) | (self.QoS << 5) | (self.Retain << 4) | \
         (self.Will << 3) | (self.CleanSession << 2) | self.TopicIdType )
    #print "Flags - pack", str(bin(ord(buffer))), len(buffer)
    return buffer
  
  def unpack(self, buffer):
    "unpack data from string buffer into separate fields"
    b0 = ord(buffer[0])
    #print "Flags - unpack", str(bin(b0)), len(buffer), buffer
    self.DUP = ((b0 >> 7) & 0x01) == 1
    self.QoS = (b0 >> 5) & 0x03
    self.Retain = ((b0 >> 4) & 0x01) == 1
    self.Will = ((b0 >> 3) & 0x01) == 1
    self.CleanSession = ((b0 >> 2) & 0x01) == 1
    self.TopicIdType = (b0 & 0x03)
    return 1

class MessageHeaders:

  def __init__(self, aMsgType):
    self.Length = 0
    self.MsgType = aMsgType

  def __eq__(self, mh):
    return self.Length == mh.Length and self.MsgType == mh.MsgType

  def __str__(self):
    "return printable stresentation of our data"
    return "Length "+str(self.Length) + ", " + packetNames[self.MsgType]

  def pack(self, length):
    "pack data into string buffer ready for transmission down socket"
    # length does not yet include the length or msgtype bytes we are going to add
    buffer = self.encode(length) + chr(self.MsgType)
    return buffer

  def encode(self, length):
    self.Length = length + 2
    assert 2 <= self.Length <= 65535
    if self.Length < 256:
      buffer = chr(self.Length)
      #print "length", self.Length
    else:
      self.Length += 2
      buffer = chr(1) + writeInt16(self.Length)
    return buffer

  def unpack(self, buffer):
    "unpack data from string buffer into separate fields"
    (self.Length, bytes) = self.decode(buffer)
    self.MsgType = ord(buffer[bytes])
    return bytes + 1

  def decode(self, buffer):
    value = ord(buffer[0])
    if value > 1:
      bytes = 1
    else:
      value = readInt16(buffer[1:])
      bytes = 3
    return (value, bytes)

def writeUTF(aString):
  return writeInt16(len(aString)) + aString

def readUTF(buffer):
  length = readInt16(buffer)
  return buffer[2:2+length]


class Packets:

  def pack(self):
    return self.mh.pack(0)

  def __str__(self):
    return str(self.mh)

  def __eq__(self, packet):
    return False if packet == None else self.mh == packet.mh
  
  def __ne__(self, packet):
    return not self.__eq__(packet)

class Advertises(Packets):

  def __init__(self, buffer=None):
    self.mh = MessageHeaders(ADVERTISE)
    self.GwId = 0     # 1 byte
    self.Duration = 0 # 2 bytes
    if buffer:
      self.unpack(buffer)
      
  def pack(self):
    buffer = chr(self.GwId) + writeInt16(self.Duration)
    return self.mh.pack(len(buffer)) + buffer
  
  def unpack(self, buffer):
    pos = self.mh.unpack(buffer)
    assert self.mh.MsgType == ADVERTISE
    self.GwId = ord(buffer[pos])
    pos += 1
    self.Duration = readInt16(buffer[pos:])
    
  def __str__(self):
    return str(self.mh) + " GwId "+str(self.GwId)+" Duration "+str(self.Duration)
    
  def __eq__(self, packet):
    return Packets.__eq__(self, packet) and \
           self.GwId == packet.GwId and \
           self.Duration == packet.Duration
  
  
class SearchGWs(Packets):

  def __init__(self, buffer=None):
    self.mh = MessageHeaders(SEARCHGW)
    self.Radius = 0
    if buffer:
      self.unpack(buffer)
      
  def pack(self):
    buffer = writeInt16(self.Radius)
    buffer = self.mh.pack(len(buffer)) + buffer
    return buffer
  
  def unpack(self, buffer):
    pos = self.mh.unpack(buffer)
    assert self.mh.MsgType == SEARCHGW
    self.Radius = readInt16(buffer[pos:])
    
  def __str__(self):
    return str(self.mh) + " Radius "+str(self.Radius)
    
class GWInfos(Packets):

  def __init__(self, buffer=None):
    self.mh = MessageHeaders(GWINFO)
    self.GwId = 0  # 1 byte
    self.GwAdd = None # optional
    if buffer:
      self.unpack(buffer)
      
  def pack(self):
    buffer = chr(self.GwId)
    if self.GwAdd:
      buffer += self.GwAdd
    buffer = self.mh.pack(len(buffer)) + buffer
    return buffer
  
  def unpack(self, buffer):
    pos = self.mh.unpack(buffer)
    assert self.mh.MsgType == GWINFO
    self.GwId = buffer[pos]
    pos += 1
    if pos >= self.mh.Length:
      self.GwAdd = None
    else:
      self.GwAdd = buffer[pos:]
          
  def __str__(self):
    buf = str(self.mh) + " Radius "+str(self.GwId)
    if self.GwAdd:
      buf += " GwAdd "+self.GwAdd
    return buf

class Connects(Packets):

  def __init__(self, buffer = None):
    self.mh = MessageHeaders(CONNECT)
    self.Flags = Flags()
    self.ProtocolId = 1
    self.Duration = 30
    self.ClientId = ""
    if buffer != None:
      self.unpack(buffer)

  def pack(self):
    buffer = self.Flags.pack() + chr(self.ProtocolId) + writeInt16(self.Duration) + self.ClientId
    return self.mh.pack(len(buffer)) + buffer

  def unpack(self, buffer):
    pos = self.mh.unpack(buffer)
    assert self.mh.MsgType == CONNECT
    pos += self.Flags.unpack(buffer[pos])
    self.ProtocolId = ord(buffer[pos])
    pos += 1
    self.Duration = readInt16(buffer[pos:])
    pos += 2
    self.ClientId = buffer[pos:]

  def __str__(self):
    buf = str(self.mh) + ", " + str(self.Flags) + \
    ", ProtocolId " + str(self.ProtocolId) + \
    ", Duration " + str(self.Duration) + \
    ", ClientId " + self.ClientId
    return buf

  def __eq__(self, packet):
    rc = Packets.__eq__(self, packet) and \
           self.Flags == packet.Flags and \
           self.ProtocolId == packet.ProtocolId and \
           self.Duration == packet.Duration and \
           self.ClientId == packet.ClientId
    return rc


class Connacks(Packets):

  def __init__(self, buffer = None):
    self.mh = MessageHeaders(CONNACK)
    self.ReturnCode = 0 # 1 byte
    if buffer != None:
      self.unpack(buffer)

  def pack(self):
    buffer = chr(self.ReturnCode)
    return self.mh.pack(len(buffer)) + buffer

  def unpack(self, buffer):
    pos = self.mh.unpack(buffer)
    assert self.mh.MsgType == CONNACK
    self.ReturnCode = ord(buffer[pos])

  def __str__(self):
    return str(self.mh)+", ReturnCode "+str(self.ReturnCode)

  def __eq__(self, packet):
    return Packets.__eq__(self, packet) and \
           self.ReturnCode == packet.ReturnCode


class WillTopicReqs(Packets):

  def __init__(self, buffer = None):
    self.mh = MessageHeaders(WILLTOPICREQ)
    if buffer != None:
      self.unpack(buffer)

  def unpack(self, buffer):
    pos = self.mh.unpack(buffer)
    assert self.mh.MsgType == WILLTOPICREQ
    
    
class WillTopics(Packets):

  def __init__(self, buffer = None):
    self.mh = MessageHeaders(WILLTOPIC)
    self.flags = Flags()
    self.WillTopic = ""
    if buffer != None:
      self.unpack(buffer)

  def pack(self):
    buffer = self.flags.pack() + self.WillTopic
    return self.mh.pack(len(buffer)) + buffer

  def unpack(self, buffer):
    pos = self.mh.unpack(buffer)
    assert self.mh.MsgType == WILLTOPIC
    pos += self.flags.unpack(buffer[pos:])
    self.WillTopic = buffer[pos:self.mh.Length]

  def __str__(self):
    return str(self.mh)+", Flags "+str(self.flags)+", WillTopic "+self.WillTopic

  def __eq__(self, packet):
    return Packets.__eq__(self, packet) and \
           self.flags == packet.flags and \
           self.WillTopic == packet.WillTopic
          
class WillMsgReqs(Packets):

  def __init__(self, buffer = None):
    self.mh = MessageHeaders(WILLMSGREQ)
    if buffer != None:
      self.unpack(buffer)

  def unpack(self, buffer):
    pos = self.mh.unpack(buffer)
    assert self.mh.MsgType == WILLMSGREQ
    
    
class WillMsgs(Packets):

  def __init__(self, buffer = None):
    self.mh = MessageHeaders(WILLMSG)
    self.WillMsg = ""
    if buffer != None:
      self.unpack(buffer)

  def pack(self):
    return self.mh.pack(len(self.WillMsg)) + self.WillMsg

  def unpack(self, buffer):
    pos = self.mh.unpack(buffer)
    assert self.mh.MsgType == WILLMSG
    self.WillMsg = buffer[pos:self.mh.Length]

  def __str__(self):
    return str(self.mh)+", WillMsg "+self.WillMsg

  def __eq__(self, packet):
    return Packets.__eq__(self, packet) and \
           self.WillMsg == packet.WillMsg
          
class Registers(Packets):

  def __init__(self, buffer = None):
    self.mh = MessageHeaders(REGISTER)
    self.TopicId = 0
    self.MsgId = 0
    self.TopicName = ""
    if buffer != None:
      self.unpack(buffer)

  def pack(self):
    buffer = writeInt16(self.TopicId) + writeInt16(self.MsgId) + self.TopicName
    return self.mh.pack(len(buffer)) + buffer

  def unpack(self, buffer):
    pos = self.mh.unpack(buffer)
    assert self.mh.MsgType == REGISTER
    self.TopicId = readInt16(buffer[pos:])
    pos += 2
    self.MsgId = readInt16(buffer[pos:])
    pos += 2
    self.TopicName = buffer[pos:self.mh.Length]

  def __str__(self):
    return str(self.mh)+", TopicId "+str(self.TopicId)+", MsgId "+str(self.MsgId)+", TopicName "+self.TopicName

  def __eq__(self, packet):
    return Packets.__eq__(self, packet) and \
           self.TopicId == packet.TopicId and \
           self.MsgId == packet.MsgId and \
           self.TopicName == packet.TopicName


class Regacks(Packets):

  def __init__(self, buffer = None):
    self.mh = MessageHeaders(REGACK)
    self.TopicId = 0
    self.MsgId = 0
    self.ReturnCode = 0 # 1 byte
    if buffer != None:
      self.unpack(buffer)

  def pack(self):
    buffer = writeInt16(self.TopicId) + writeInt16(self.MsgId) + chr(self.ReturnCode)
    return self.mh.pack(len(buffer)) + buffer

  def unpack(self, buffer):
    pos = self.mh.unpack(buffer)
    assert self.mh.MsgType == REGACK
    self.TopicId = readInt16(buffer[pos:])
    pos += 2
    self.MsgId = readInt16(buffer[pos:])
    pos += 2
    self.ReturnCode = ord(buffer[pos])

  def __str__(self):
    return str(self.mh)+", TopicId "+str(self.TopicId)+", MsgId "+str(self.MsgId)+", ReturnCode "+str(self.ReturnCode)

  def __eq__(self, packet):
    return Packets.__eq__(self, packet) and \
           self.TopicId == packet.TopicId and \
           self.MsgId == packet.MsgId and \
           self.ReturnCode == packet.ReturnCode


class Publishes(Packets):

  def __init__(self, buffer = None):
    self.mh = MessageHeaders(PUBLISH)
    self.Flags = Flags()
    self.TopicId = 0 # 2 bytes
    self.TopicName = ""
    self.MsgId = 0 # 2 bytes
    self.Data = ""
    if buffer != None:
      self.unpack(buffer)

  def pack(self):
    buffer = self.Flags.pack()
    if self.Flags.TopicIdType in [TOPIC_NORMAL, TOPIC_PREDEFINED, 3]:
      #print "topic id is", self.TopicId
      buffer += writeInt16(self.TopicId)
    elif self.Flags.TopicIdType == TOPIC_SHORTNAME:
      buffer += (self.TopicName + "  ")[0:2]
    buffer += writeInt16(self.MsgId) + self.Data
    return self.mh.pack(len(buffer)) + buffer

  def unpack(self, buffer):
    pos = self.mh.unpack(buffer)
    assert self.mh.MsgType == PUBLISH
    pos += self.Flags.unpack(buffer[pos:])
    
    self.TopicId = 0
    self.TopicName = ""
    if self.Flags.TopicIdType in [TOPIC_NORMAL, TOPIC_PREDEFINED]:
      self.TopicId = readInt16(buffer[pos:])
    elif self.Flags.TopicIdType == TOPIC_SHORTNAME:
      self.TopicName = buffer[pos:pos+2]
    pos += 2
    self.MsgId = readInt16(buffer[pos:])
    pos += 2
    self.Data = buffer[pos:self.mh.Length]

  def __str__(self):
    return str(self.mh)+", Flags "+str(self.Flags)+", TopicId "+str(self.TopicId)+", MsgId "+str(self.MsgId)+", Data "+self.Data

  def __eq__(self, packet):
    return Packets.__eq__(self, packet) and \
         self.Flags == packet.Flags and \
         self.TopicId == packet.TopicId and \
         self.MsgId == packet.MsgId and \
         self.Data == packet.Data


class Pubacks(Packets):

  def __init__(self, buffer = None):
    self.mh = MessageHeaders(PUBACK)
    self.TopicId = 0
    self.MsgId = 0
    self.ReturnCode = 0 # 1 byte
    if buffer != None:
      self.unpack(buffer)

  def pack(self):
    buffer = writeInt16(self.TopicId) + writeInt16(self.MsgId) + chr(self.ReturnCode)
    return self.mh.pack(len(buffer)) + buffer

  def unpack(self, buffer):
    pos = self.mh.unpack(buffer)
    assert self.mh.MsgType == PUBACK
    self.TopicId = readInt16(buffer[pos:])
    pos += 2
    self.MsgId = readInt16(buffer[pos:])
    pos += 2
    self.ReturnCode = ord(buffer[pos])

  def __str__(self):
    return str(self.mh)+", TopicId "+str(self.TopicId)+" , MsgId "+str(self.MsgId)+", ReturnCode "+str(self.ReturnCode)

  def __eq__(self, packet):
    return Packets.__eq__(self, packet) and \
           self.TopicId == packet.TopicId and \
           self.MsgId == packet.MsgId and \
           self.ReturnCode == packet.ReturnCode
          
          
class Pubrecs(Packets):

  def __init__(self, buffer = None):
    self.mh = MessageHeaders(PUBREC)
    self.MsgId = 0
    if buffer != None:
      self.unpack(buffer)

  def pack(self):
    return self.mh.pack(2) + writeInt16(self.MsgId)

  def unpack(self, buffer):
    pos = self.mh.unpack(buffer)
    assert self.mh.MsgType == PUBREC
    self.MsgId = readInt16(buffer[pos:])

  def __str__(self):
    return str(self.mh)+" , MsgId "+str(self.MsgId)

  def __eq__(self, packet):
    return Packets.__eq__(self, packet) and self.MsgId == packet.MsgId
    
class Pubrels(Packets):

  def __init__(self, buffer = None):
    self.mh = MessageHeaders(PUBREL)
    self.MsgId = 0
    if buffer != None:
      self.unpack(buffer)

  def pack(self):
    return self.mh.pack(2) + writeInt16(self.MsgId)

  def unpack(self, buffer):
    pos = self.mh.unpack(buffer)
    assert self.mh.MsgType == PUBREL
    self.MsgId = readInt16(buffer[pos:])

  def __str__(self):
    return str(self.mh)+" , MsgId "+str(self.MsgId)

  def __eq__(self, packet):
    return Packets.__eq__(self, packet) and self.MsgId == packet.MsgId


class Pubcomps(Packets):

  def __init__(self, buffer = None):
    self.mh = MessageHeaders(PUBCOMP)
    self.MsgId = 0
    if buffer != None:
      self.unpack(buffer)

  def pack(self):
    return self.mh.pack(2) + writeInt16(self.MsgId)

  def unpack(self, buffer):
    pos = self.mh.unpack(buffer)
    assert self.mh.MsgType == PUBCOMP
    self.MsgId = readInt16(buffer[pos:])

  def __str__(self):
    return str(self.mh)+" , MsgId "+str(self.MsgId)

  def __eq__(self, packet):
    return Packets.__eq__(self, packet) and self.MsgId == packet.MsgId
    
    
class Subscribes(Packets):

  def __init__(self, buffer = None):
    self.mh = MessageHeaders(SUBSCRIBE)
    self.Flags = Flags()
    self.MsgId = 0 # 2 bytes
    self.TopicId = 0 # 2 bytes
    self.TopicName = ""
    if buffer != None:
      self.unpack(buffer)

  def pack(self):
    buffer = self.Flags.pack() + writeInt16(self.MsgId)
    if self.Flags.TopicIdType == TOPIC_PREDEFINED:
      buffer += writeInt16(self.TopicId)
    elif self.Flags.TopicIdType in [TOPIC_NORMAL, TOPIC_SHORTNAME]:
      buffer += self.TopicName
    return self.mh.pack(len(buffer)) + buffer


  def unpack(self, buffer):
    pos = self.mh.unpack(buffer)
    assert self.mh.MsgType == SUBSCRIBE
    pos += self.Flags.unpack(buffer[pos:])
    self.MsgId = readInt16(buffer[pos:])
    pos += 2
    self.TopicId = 0
    self.TopicName = ""
    if self.Flags.TopicIdType == TOPIC_PREDEFINED:
      self.TopicId = readInt16(buffer[pos:])
    elif self.Flags.TopicIdType in [TOPIC_NORMAL, TOPIC_SHORTNAME]:
      self.TopicName = buffer[pos:pos+2]

  def __str__(self):
    buffer = str(self.mh)+", Flags "+str(self.Flags)+", MsgId "+str(self.MsgId)
    if self.Flags.TopicIdType == 0:
      buffer += ", TopicName "+self.TopicName
    elif self.Flags.TopicIdType == 1:
      buffer += ", TopicId "+str(self.TopicId)
    elif self.Flags.TopicIdType == 2:
      buffer += ", TopicId "+self.TopicId
    return buffer

  def __eq__(self, packet):
    if self.Flags.TopicIdType == 0:
      rc = self.TopicName == packet.TopicName
    else:
      rc = self.TopicId == packet.TopicId
    return Packets.__eq__(self, packet) and \
         self.Flags == packet.Flags and \
         self.MsgId == packet.MsgId and rc


class Subacks(Packets):

  def __init__(self, buffer = None):
    self.mh = MessageHeaders(SUBACK)
    self.Flags = Flags() # 1 byte
    self.TopicId = 0 # 2 bytes
    self.MsgId = 0 # 2 bytes
    self.ReturnCode = 0 # 1 byte
    if buffer != None:
      self.unpack(buffer)

  def pack(self):
    buffer = self.Flags.pack() + writeInt16(self.TopicId) + writeInt16(self.MsgId) + chr(self.ReturnCode)
    return self.mh.pack(len(buffer)) + buffer

  def unpack(self, buffer):
    pos = self.mh.unpack(buffer)
    assert self.mh.MsgType == SUBACK
    pos += self.Flags.unpack(buffer[pos:])
    self.TopicId = readInt16(buffer[pos:])
    pos += 2
    self.MsgId = readInt16(buffer[pos:])
    pos += 2
    self.ReturnCode = ord(buffer[pos])

  def __str__(self):
    return str(self.mh)+", Flags "+str(self.Flags)+", TopicId "+str(self.TopicId)+" , MsgId "+str(self.MsgId)+", ReturnCode "+str(self.ReturnCode)

  def __eq__(self, packet):
    return Packets.__eq__(self, packet) and \
           self.Flags == packet.Flags and \
           self.TopicId == packet.TopicId and \
           self.MsgId == packet.MsgId and \
           self.ReturnCode == packet.ReturnCode


class Unsubscribes(Packets):

  def __init__(self, buffer = None):
    self.mh = MessageHeaders(UNSUBSCRIBE)
    self.Flags = Flags()
    self.MsgId = 0 # 2 bytes
    self.TopicId = 0 # 2 bytes
    self.TopicName = ""
    if buffer != None:
      self.unpack(buffer)

  def pack(self):
    buffer = self.Flags.pack() + writeInt16(self.MsgId)
    if self.Flags.TopicIdType == 0:
      buffer += self.TopicName
    elif self.Flags.TopicIdType == 1:
      buffer += writeInt16(self.TopicId)
    elif self.Flags.TopicIdType == 2:
      buffer += self.TopicId
    return self.mh.pack(len(buffer)) + buffer

  def unpack(self, buffer):
    pos = self.mh.unpack(buffer)
    assert self.mh.MsgType == UNSUBSCRIBE
    pos += self.Flags.unpack(buffer[pos:])
    self.MsgId = readInt16(buffer[pos:])
    pos += 2
    self.TopicId = 0
    self.TopicName = ""
    if self.Flags.TopicIdType == 0:
      self.TopicName = buffer[pos:self.mh.Length]
    elif self.Flags.TopicIdType == 1:
      self.TopicId = readInt16(buffer[pos:])
    elif self.Flags.TopicIdType == 3:
      self.TopicId = buffer[pos:pos+2]

  def __str__(self):
    buffer = str(self.mh)+", Flags "+str(self.Flags)+", MsgId "+str(self.MsgId)
    if self.Flags.TopicIdType == 0:
      buffer += ", TopicName "+self.TopicName
    elif self.Flags.TopicIdType == 1:
      buffer += ", TopicId "+str(self.TopicId)
    elif self.Flags.TopicIdType == 2:
      buffer += ", TopicId "+self.TopicId
    return buffer

  def __eq__(self, packet):
    return Packets.__eq__(self, packet) and \
         self.Flags == packet.Flags and \
         self.MsgId == packet.MsgId and \
         self.TopicId == packet.TopicId and \
         self.TopicName == packet.TopicName

class Unsubacks(Packets):

  def __init__(self, buffer = None):
    self.mh = MessageHeaders(UNSUBACK)
    self.MsgId = 0
    if buffer != None:
      self.unpack(buffer)

  def pack(self):
    return self.mh.pack(2) + writeInt16(self.MsgId)

  def unpack(self, buffer):
    pos = self.mh.unpack(buffer)
    assert self.mh.MsgType == UNSUBACK
    self.MsgId = readInt16(buffer[pos:])

  def __str__(self):
    return str(self.mh)+" , MsgId "+str(self.MsgId)

  def __eq__(self, packet):
    return Packets.__eq__(self, packet) and self.MsgId == packet.MsgId


class Pingreqs(Packets):

  def __init__(self, buffer = None):
    self.mh = MessageHeaders(PINGREQ)
    self.ClientId = None
    if buffer != None:
      self.unpack(buffer)

  def pack(self):
    if self.ClientId:
      buf = self.mh.pack(len(self.ClientId)) + self.ClientId
    else:
      buf = self.mh.pack(0)
    return buf

  def unpack(self, buffer):
    pos = self.mh.unpack(buffer)
    assert self.mh.MsgType == PINGREQ
    self.ClientId = buffer[pos:self.mh.Length]
    if self.ClientId == '':
      self.ClientId = None

  def __str__(self):
    buf = str(self.mh)
    if self.ClientId:
      buf += ", ClientId "+self.ClientId
    return buf

  def __eq__(self, packet):
    return Packets.__eq__(self, packet) and \
           self.ClientId == packet.ClientId
          

class Pingresps(Packets):

  def __init__(self, buffer = None):
    self.mh = MessageHeaders(PINGRESP)
    if buffer != None:
      self.unpack(buffer)

  def unpack(self, buffer):
    pos = self.mh.unpack(buffer)
    assert self.mh.MsgType == PINGRESP
  
class Disconnects(Packets):

  def __init__(self, buffer = None):
    self.mh = MessageHeaders(DISCONNECT)
    self.Duration = None
    if buffer != None:
      self.unpack(buffer)

  def pack(self):
    if self.Duration:
      buf = self.mh.pack(2) + writeInt16(self.Duration)
    else:
      buf = self.mh.pack(0)
    return buf

  def unpack(self, buffer):
    pos = self.mh.unpack(buffer)
    assert self.mh.MsgType == DISCONNECT
    buf = buffer[pos:self.mh.Length]
    if buf == '':
      self.Duration = None
    else:
      self.Duration = readInt16(buffer[pos:])

  def __str__(self):
    buf = str(self.mh)
    if self.Duration:
      buf += ", Duration "+str(self.Duration)
    return buf

  def __eq__(self, packet):
    return Packets.__eq__(self, packet) and \
           self.Duration == packet.Duration
          
class WillTopicUpds(Packets):

  def __init__(self, buffer = None):
    self.mh = MessageHeaders(WILLTOPICUPD)
    self.flags = Flags()
    self.WillTopic = ""
    if buffer != None:
      self.unpack(buffer)

  def pack(self):
    buffer = self.flags.pack() + self.WillTopic
    return self.mh.pack(len(buffer)) + buffer

  def unpack(self, buffer):
    pos = self.mh.unpack(buffer)
    assert self.mh.MsgType == WILLTOPICUPD
    pos += self.flags.unpack(buffer[pos:])
    self.WillTopic = buffer[pos:self.mh.Length]

  def __str__(self):
    return str(self.mh)+", Flags "+str(self.flags)+", WillTopic "+self.WillTopic

  def __eq__(self, packet):
    return Packets.__eq__(self, packet) and \
           self.flags == packet.flags and \
           self.WillTopic == packet.WillTopic
          
class WillMsgUpds(Packets):

  def __init__(self, buffer = None):
    self.mh = MessageHeaders(WILLMSGUPD)
    self.WillMsg = ""
    if buffer != None:
      self.unpack(buffer)

  def pack(self):
    return self.mh.pack(len(self.WillMsg)) + self.WillMsg

  def unpack(self, buffer):
    pos = self.mh.unpack(buffer)
    assert self.mh.MsgType == WILLMSGUPD
    self.WillMsg = buffer[pos:self.mh.Length]

  def __str__(self):
    return str(self.mh)+", WillMsg "+self.WillMsg

  def __eq__(self, packet):
    return Packets.__eq__(self, packet) and \
           self.WillMsg == packet.WillMsg
          
class WillTopicResps(Packets):

  def __init__(self, buffer = None):
    self.mh = MessageHeaders(WILLTOPICRESP)
    self.ReturnCode = 0
    if buffer != None:
      self.unpack(buffer)

  def pack(self):
    buffer = writeInt16(self.ReturnCode)
    return self.mh.pack(len(buffer)) + buffer

  def unpack(self, buffer):
    pos = self.mh.unpack(buffer)
    assert self.mh.MsgType == WILLTOPICRESP
    self.ReturnCode = readInt16(buffer[pos:])

  def __str__(self):
    return str(self.mh)+", ReturnCode "+str(self.ReturnCode)

  def __eq__(self, packet):
    return Packets.__eq__(self, packet) and \
           self.ReturnCode == packet.ReturnCode
          
class WillMsgResps(Packets):

  def __init__(self, buffer = None):
    self.mh = MessageHeaders(WILLMSGRESP)
    self.ReturnCode = 0
    if buffer != None:
      self.unpack(buffer)

  def pack(self):
    buffer = writeInt16(self.ReturnCode)
    return self.mh.pack(len(buffer)) + buffer

  def unpack(self, buffer):
    pos = self.mh.unpack(buffer)
    assert self.mh.MsgType == WILLMSGRESP
    self.returnCode = readInt16(buffer[pos:])

  def __str__(self):
    return str(self.mh)+", ReturnCode "+str(self.ReturnCode)

  def __eq__(self, packet):
    return Packets.__eq__(self, packet) and \
           self.ReturnCode == packet.ReturnCode

objects = [Advertises, SearchGWs, GWInfos, None,
           Connects, Connacks,
           WillTopicReqs, WillTopics, WillMsgReqs, WillMsgs, 
           Registers, Regacks, 
           Publishes, Pubacks, Pubcomps, Pubrecs, Pubrels, None,
           Subscribes, Subacks, Unsubscribes, Unsubacks,
           Pingreqs, Pingresps, Disconnects, None,
           WillTopicUpds, WillTopicResps, WillMsgUpds, WillMsgResps]

def unpackPacket((buffer, address)):
  if MessageType(buffer) != None:
    packet = objects[MessageType(buffer)]()
    packet.unpack(buffer)
  else:
    packet = None
  return packet, address

  

import time
import sys
import socket
import traceback



debug = False

class Receivers:

  def __init__(self, socket):
    print "initializing receiver"
    self.socket = socket
    self.connected = False
    self.observe = None
    self.observed = []

    self.inMsgs = {}
    self.outMsgs = {}

    self.puback = Pubacks()
    self.pubrec = Pubrecs()
    self.pubrel = Pubrels()
    self.pubcomp = Pubcomps()

  def lookfor(self, msgType):
    self.observe = msgType

  def waitfor(self, msgType, msgId=None):
    msg = None
    count = 0
    while True:
      while len(self.observed) > 0:
        msg = self.observed.pop(0)
        if msg.mh.MsgType == msgType and (msgId == None or msg.MsgId == msgId):
          break
        else:
          msg = None
      if msg != None:
        break
      time.sleep(0.2)
      count += 1
      if count == 25:
        msg = None
        break
    self.observe = None
    return msg

  def receive(self, callback=None):
    packet = None
    try:
      packet, address = unpackPacket(getPacket(self.socket))
    except:
      if sys.exc_info()[0] != socket.timeout:
        print "unexpected exception", sys.exc_info()
        raise sys.exc_info()
    if packet == None:
      time.sleep(0.1)
      return
    elif debug:
      print packet

    if self.observe == packet.mh.MsgType:
      print "observed", packet
      self.observed.append(packet)

    elif packet.mh.MsgType == ADVERTISE:
      if hasattr(callback, "advertise"):
        callback.advertise(address, packet.GwId, packet.Duration)

    elif packet.mh.MsgType == REGISTER:
      if callback and hasattr(callback, "register"):
        callback.register(packet.TopicId, packet.Topicname)

    elif packet.mh.MsgType == PUBACK:
      "check if we are expecting a puback"
      if self.outMsgs.has_key(packet.MsgId) and \
        self.outMsgs[packet.MsgId].Flags.QoS == 1:
        del self.outMsgs[packet.MsgId]
        if hasattr(callback, "published"):
          callback.published(packet.MsgId)
      else:
        raise Exception("No QoS 1 message with message id "+str(packet.MsgId)+" sent")

    elif packet.mh.MsgType == PUBREC:
      if self.outMsgs.has_key(packet.MsgId):
        self.pubrel.MsgId = packet.MsgId
        self.socket.send(self.pubrel.pack())
      else:
        raise Exception("PUBREC received for unknown msg id "+ \
                    str(packet.MsgId))

    elif packet.mh.MsgType == PUBREL:
      "release QOS 2 publication to client, & send PUBCOMP"
      msgid = packet.MsgId
      if not self.inMsgs.has_key(msgid):
        pass # what should we do here?
      else:
        pub = self.inMsgs[packet.MsgId]
        if callback == None or \
           callback.messageArrived(pub.TopicName, pub.Data, 2, pub.Flags.Retain, pub.MsgId):
          del self.inMsgs[packet.MsgId]
          self.pubcomp.MsgId = packet.MsgId
          self.socket.send(self.pubcomp.pack())
        if callback == None:
          return (pub.TopicName, pub.Data, 2, pub.Flags.Retain, pub.MsgId)

    elif packet.mh.MsgType == PUBCOMP:
      "finished with this message id"
      if self.outMsgs.has_key(packet.MsgId):
        del self.outMsgs[packet.MsgId]
        if hasattr(callback, "published"):
          callback.published(packet.MsgId)
      else:
        raise Exception("PUBCOMP received for unknown msg id "+ \
                    str(packet.MsgId))

    elif packet.mh.MsgType == PUBLISH:
      "finished with this message id"
      if packet.Flags.QoS in [0, 3]:
        qos = packet.Flags.QoS
        topicname = packet.TopicName
        data = packet.Data
        if qos == 3:
          qos = -1
          if packet.Flags.TopicIdType == TOPICID:
            topicname = packet.Data[:packet.TopicId]
            data = packet.Data[packet.TopicId:]
        if callback == None:
          return (topicname, data, qos, packet.Flags.Retain, packet.MsgId)
        else:
          callback.messageArrived(topicname, data, qos, packet.Flags.Retain, packet.MsgId)
      elif packet.Flags.QoS == 1:
        if callback == None:
          return (packet.topicName, packet.Data, 1,
                           packet.Flags.Retain, packet.MsgId)
        else:
          if callback.messageArrived(packet.TopicName, packet.Data, 1,
                           packet.Flags.Retain, packet.MsgId):
            self.puback.MsgId = packet.MsgId
            self.socket.send(self.puback.pack())
      elif packet.Flags.QoS == 2:
        self.inMsgs[packet.MsgId] = packet
        self.pubrec.MsgId = packet.MsgId
        self.socket.send(self.pubrec.pack())

    else:
      raise Exception("Unexpected packet"+str(packet))
    return packet

  def __call__(self, callback):
    try:
      while True:
        self.receive(callback)
    except:
      if sys.exc_info()[0] != socket.error:
        print "unexpected exception", sys.exc_info()
        traceback.print_exc()

"""
/*******************************************************************************
 * Copyright (c) 2011, 2013 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 *
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *   http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *    Ian Craggs - initial API and implementation and/or initial documentation
 *******************************************************************************/
"""

import socket
import thread
import types
import struct


class Client:
    def __init__(self, clientid, host="localhost", port=1883):
        self.clientid = clientid
        self.host = host
        self.port = port
        self.msgid = 1
        self.callback = None
        self.__receiver = None

    def start(self):
        self.sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM, socket.IPPROTO_UDP)
        self.sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        self.sock.bind((self.host, self.port))
        mreq = struct.pack("4sl", socket.inet_aton(self.host), socket.INADDR_ANY)

        self.sock.setsockopt(socket.IPPROTO_IP, socket.IP_ADD_MEMBERSHIP, mreq)

        self.startReceiver()

    def stop(self):
        self.stopReceiver()

    def __nextMsgid(self):
        def getWrappedMsgid():
            id = self.msgid + 1
            if id == 65535:
                id = 1
            return id

        if len(self.__receiver.outMsgs) >= 65535:
            raise "No slots left!!"
        else:
            self.msgid = getWrappedMsgid()
            while self.__receiver.outMsgs.has_key(self.msgid):
                self.msgid = getWrappedMsgid()
        return self.msgid


    def registerCallback(self, callback):
        self.callback = callback


    def connect(self, cleansession=True):
        self.sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        #self.sock.settimeout(5.0)

        self.sock.connect((self.host, self.port))

        connect = Connects()
        connect.ClientId = self.clientid
        connect.CleanSession = cleansession
        connect.KeepAliveTimer = 0
        self.sock.send(connect.pack())

        response, address = unpackPacket(getPacket(self.sock))
        assert response.mh.MsgType == CONNACK

        self.startReceiver()


    def startReceiver(self):
        self.__receiver = Receivers(self.sock)
        if self.callback:
            id = thread.start_new_thread(self.__receiver, (self.callback,))


    def waitfor(self, msgType, msgId=None):
        if self.__receiver:
            msg = self.__receiver.waitfor(msgType, msgId)
        else:
            msg = self.__receiver.receive()
            while msg.mh.MsgType != msgType and (msgId == None or msgId == msg.MsgId):
                msg = self.__receiver.receive()
        return msg


    def subscribe(self, topic, qos=2):
        subscribe = Subscribes()
        subscribe.MsgId = self.__nextMsgid()
        if type(topic) == types.StringType:
            subscribe.TopicName = topic
            if len(topic) > 2:
                subscribe.Flags.TopicIdType = TOPIC_NORMAL
            else:
                subscribe.Flags.TopicIdType = TOPIC_SHORTNAME
        else:
            subscribe.TopicId = topic # should be int
            subscribe.Flags.TopicIdType = TOPIC_PREDEFINED
        subscribe.Flags.QoS = qos
        if self.__receiver:
            self.__receiver.lookfor(SUBACK)
        self.sock.send(subscribe.pack())
        msg = self.waitfor(SUBACK, subscribe.MsgId)
        return msg.ReturnCode, msg.TopicId


    def unsubscribe(self, topics):
        unsubscribe = Unsubscribes()
        unsubscribe.MsgId = self.__nextMsgid()
        unsubscribe.data = topics
        if self.__receiver:
            self.__receiver.lookfor(UNSUBACK)
        self.sock.send(unsubscribe.pack())
        msg = self.waitfor(UNSUBACK, unsubscribe.MsgId)


    def register(self, topicName):
        register = Registers()
        register.TopicName = topicName
        if self.__receiver:
            self.__receiver.lookfor(REGACK)
        self.sock.send(register.pack())
        msg = self.waitfor(REGACK, register.MsgId)
        return msg.TopicId


    def publish(self, topic, payload, qos=0, retained=False):
        publish = Publishes()
        publish.Flags.QoS = qos
        publish.Flags.Retain = retained
        if type(topic) == types.StringType:
            publish.Flags.TopicIdType = TOPIC_SHORTNAME
            publish.TopicName = topic
        else:
            publish.Flags.TopicIdType = TOPIC_NORMAL
            publish.TopicId = topic
        if qos in [-1, 0]:
            publish.MsgId = 0
        else:
            publish.MsgId = self.__nextMsgid()
            #print "MsgId", publish.MsgId
            self.__receiver.outMsgs[publish.MsgId] = publish
        publish.Data = payload
        self.sock.send(publish.pack())
        return publish.MsgId


    def disconnect(self):
        disconnect = Disconnects()
        if self.__receiver:
            self.__receiver.lookfor(DISCONNECT)
        self.sock.send(disconnect.pack())
        msg = self.waitfor(DISCONNECT)


    def stopReceiver(self):
        self.sock.close() # this will stop the receiver too
        assert self.__receiver.inMsgs == {}
        assert self.__receiver.outMsgs == {}
        self.__receiver = None

    def receive(self):
        return self.__receiver.receive()


def publish(topic, payload, retained=False, port=1883, host="localhost"):
    publish = Publishes()
    publish.Flags.QoS = 3
    publish.Flags.Retain = retained
    if type(topic) == types.StringType:
        if len(topic) > 2:
            publish.Flags.TopicIdType = TOPIC_NORMAL
            publish.TopicId = len(topic)
            payload = topic + payload
        else:
            publish.Flags.TopicIdType = TOPIC_SHORTNAME
            publish.TopicName = topic
    else:
        publish.Flags.TopicIdType = TOPIC_NORMAL
        publish.TopicId = topic
    publish.MsgId = 0
    #print "payload", payload
    publish.Data = payload
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    sock.sendto(publish.pack(), (host, port))
    sock.close()
    return




import time
import sys


class Callback:
    def __init__(self):
        self.messages_number = 0
        self.average_delay = 0.0
        self.first_message = 0l
        self.last_message = 0l
        self.order = True
        self.old_msg = 0l

    def connectionLost(self, cause):
        print "default connectionLost", cause

    def messageArrived(self, topicName, payload, qos, retained, msgid):
        msg_time = long(payload)
        self.last_message = long(round(time.time() * 1000))

        if self.first_message == 0l:
            self.first_message = long(round(time.time() * 1000))

        self.order = self.order and (msg_time >= self.old_msg)
        self.old_msg = msg_time

        new_delay = self.last_message - msg_time

        num = float(self.messages_number)
        self.average_delay = (num / (num + 1.0)) * self.average_delay + float(new_delay) / (num + 1.0)

        self.messages_number += 1
        return True

    def deliveryComplete(self, msgid):
        pass

    def advertise(self, address, gwid, duration):
        pass

    def register(self, topicid, topicName):
        pass


if __name__ == "__main__":

    aclient = Client("receiver", host="127.0.0.1", port=2884)
    callback = Callback()
    aclient.registerCallback(callback)
    aclient.connect()

    rc, topic1 = aclient.subscribe("test")

    aclient.startReceiver()
    data = sys.stdin.readline()

    print callback.messages_number
    print callback.average_delay
    print callback.order
    print callback.last_message - callback.first_message

    aclient.stopReceiver()