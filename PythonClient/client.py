#!/usr/bin/env python2.7
import socket
import time
import threading
import wx
import select

connectionState = 0
dataBuffer = ""
sentCommandHistory = []
sentCommandHistoryId = 0

EVENT_DISCONNECTED = wx.NewEventType()
EVENT_NEWDATA = wx.NewEventType()

class SocketClientThread(threading.Thread):
    def __init__(self, conn, parent):
        threading.Thread.__init__(self)
        self.conn = conn
        self.parent = parent

    def run(self):
        global connectionState, dataBuffer
        while connectionState:
            # this will try to recieve data continuously
            try:
                # this makes sure there's someting to read from the server before actually reading it
                # it's necessary because without it will keep blocking even with a closed connection
                if select.select([self.conn], [], []):
                    # this is blocking
                    data = self.conn.recv(1024)
                
                # connection lost
                if not data:
                    wx.PostEvent(self.parent, wx.PyCommandEvent(EVENT_DISCONNECTED, -1))
                    return
                
                # remove all whitespace characters on the right side
                # append so that if there's a new packet before the previous is shown it doesn't get lost
                dataBuffer += data.rstrip()
                wx.PostEvent(self.parent, wx.PyCommandEvent(EVENT_NEWDATA, -1))
            except socket.timeout, e:
                #print e.args[0]
                continue
            except Exception as e:
                #print e.args[0]
                return

    def send(self, data):
        # new line defines an end of command on the server side
        self.conn.send(data + "\n")

    def close(self):
        global connectionState
        connectionState = 0
        self.conn.close()

class SocketClientUI(wx.Frame):
    def __init__(self, parent, title):
        super(SocketClientUI, self).__init__(parent, title=title, size=(500, 500))
        
        # keyboard events
        self.Bind(wx.EVT_CHAR_HOOK, self.onKey)
        
        # bind events from socket client thread
        self.Bind(wx.PyEventBinder(EVENT_DISCONNECTED, 1), self.disconnect)
        self.Bind(wx.PyEventBinder(EVENT_NEWDATA, 1), self.updateOutput)
        
        self.InitUI()
        self.Centre()
        self.Show()
        
    def InitUI(self):
        panel = wx.Panel(self)
        sizer = wx.GridBagSizer(5, 5)
        
        # add some labels for IP and port text entries
        sizer.Add(wx.StaticText(panel, label="IP"), pos=(0, 0), flag=wx.TOP|wx.LEFT, border=5)
        sizer.Add(wx.StaticText(panel, label="Port"), pos=(0, 3), flag=wx.TOP, border=5)
        
        # set a default value for the IP
        self.tc_ip = wx.TextCtrl(panel, value="192.168.0.105")
        sizer.Add(self.tc_ip, pos=(1, 0), span=(1, 3), flag=wx.EXPAND|wx.LEFT, border=5)
        
        # port is 8888 by defaut because of reasons
        self.tc_port = wx.TextCtrl(panel, value="8888")
        sizer.Add(self.tc_port, pos=(1, 3), span=(1, 1), flag=wx.EXPAND)
        
        # click connect button for magic
        self.button_connect = wx.Button(panel, label="Connect")
        sizer.Add(self.button_connect, pos=(1, 4), flag=wx.EXPAND|wx.RIGHT, border=5)
        self.button_connect.Bind(wx.EVT_BUTTON, self.connect)
        
        # output data
        sizer.Add(wx.StaticText(panel, label="Output"), pos=(2, 0), flag=wx.TOP|wx.LEFT, border=5)
        self.output = wx.TextCtrl(panel, style = wx.TE_MULTILINE)
        #self.output.Enable(False)
        sizer.Add(self.output, pos=(3, 0), span=(1, 5), flag=wx.EXPAND|wx.LEFT|wx.RIGHT, border=5)
        
        # input entries
        sizer.Add(wx.StaticText(panel, label="Input"), pos=(4, 0), flag=wx.TOP|wx.LEFT, border=5)
        self.tc_send = wx.TextCtrl(panel)
        self.tc_send.Enable(False)
        sizer.Add(self.tc_send, pos=(5, 0), span=(1, 4), flag=wx.EXPAND|wx.LEFT|wx.BOTTOM, border=5)
        self.button_send = wx.Button(panel, label="Send")
        self.button_send.Enable(False)
        sizer.Add(self.button_send, pos=(5, 4), flag=wx.EXPAND|wx.RIGHT, border=5)
        self.button_send.Bind(wx.EVT_BUTTON, self.send)
        
        # make first column and 3rd row growable
        sizer.AddGrowableCol(1)
        sizer.AddGrowableRow(3)
        
        panel.SetSizerAndFit(sizer)
    
    def disconnect(self, event):
        global connectionState
        self.clientSocket.close()
        connectionState = 0
        
        if event != wx.EVT_BUTTON:
            wx.MessageBox('Connection lost!', 'Error', wx.OK|wx.ICON_ERROR)
        
        # update UI element statuses
        self.button_connect.SetLabel("Connect")
        self.tc_ip.Enable(True)
        self.tc_port.Enable(True)
        self.tc_send.Enable(False)
        self.tc_send.SetValue("")
        self.button_send.Enable(False)
        
        # set focus on the ip field
        wx.Window.SetFocus(self.tc_ip)

    def connect(self, event):
        global connectionState
        
        # disconnect when a socket is connected
        if connectionState:
            self.disconnect(wx.EVT_BUTTON)
            return
        
        self.clientSocket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        # set timeout in case a wrong ip is used (in seconds)
        self.clientSocket.settimeout(2)
        ip = self.tc_ip.GetValue()
        port = int(self.tc_port.GetValue())
        
        try:
            self.clientSocket.connect((ip, port))
        except socket.timeout:
            wx.MessageBox('Timeout while connecting, verify the IP and port.', 'Error', wx.OK|wx.ICON_ERROR)
            return
        except Exception, e:
            wx.MessageBox('Could not connect to socket, is the app running?', 'Error', wx.OK|wx.ICON_ERROR)
            return
        
        # start the receieveing thread
        connectionState = 1
        self.sct = SocketClientThread(self.clientSocket, self)
        self.sct.start()
        
        # update UI element statuses
        self.button_connect.SetLabel("Stop")
        self.tc_ip.Enable(False)
        self.tc_port.Enable(False)
        self.tc_send.Enable(True)
        self.button_send.Enable(True)

        # set focus on the send field
        wx.Window.SetFocus(self.tc_send)
    
    def onKey(self, event):
        global connectionState
        k = event.GetKeyCode()
        if k == wx.WXK_RETURN:
            if not connectionState and wx.Window.FindFocus() == self.tc_ip:
                # try to connect if not yet connected
                self.connect(wx.EVT_BUTTON)
            elif connectionState and wx.Window.FindFocus() == self.tc_send:
                # send data to device
                self.send(wx.EVT_BUTTON)
        elif k == wx.WXK_UP:
            # navigate up in previous commands
            self.navigateCommands(wx.WXK_UP)
        elif k == wx.WXK_DOWN:
            # navigate up in previous commands
            self.navigateCommands(wx.WXK_DOWN)
        else:
            event.Skip()
            
    def navigateCommands(self, event):
        global sentCommandHistory, sentCommandHistoryId
        if not len(sentCommandHistory):
            return
        if event == wx.WXK_UP and sentCommandHistoryId > 0:
            sentCommandHistoryId = sentCommandHistoryId - 1
            self.tc_send.SetValue(sentCommandHistory[sentCommandHistoryId])
        elif event == wx.WXK_DOWN and sentCommandHistoryId < len(sentCommandHistory) - 1:
            sentCommandHistoryId = sentCommandHistoryId + 1
            self.tc_send.SetValue(sentCommandHistory[sentCommandHistoryId])
        elif event == wx.WXK_DOWN and sentCommandHistoryId < len(sentCommandHistory):
            sentCommandHistoryId = sentCommandHistoryId + 1
            self.tc_send.SetValue("")
    
    def send(self, event):
        global sentCommandHistory, sentCommandHistoryId
        data = self.tc_send.GetValue()
        if data != "":
            self.sct.send(data)
            sentCommandHistory.append(data)
            sentCommandHistoryId = len(sentCommandHistory)
            self.tc_send.SetValue("")
            self.output.AppendText("> " + data + "\n")
    
    def updateOutput(self, event):
        global dataBuffer
        self.output.AppendText("< " + dataBuffer + "\n")
        dataBuffer = ""

if __name__ == '__main__':
    app = wx.App()
    SocketClientUI(None, title="Python Terminal Client")
    app.MainLoop()
