package com.islandpacific.monitoring.servicescheduler;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.activation.DataHandler;
import javax.mail.*;
import javax.mail.internet.*;
import javax.mail.util.ByteArrayDataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.islandpacific.monitoring.common.AppLogger;

public class EmailService {

    private static final Logger logger = AppLogger.getLogger();
    private static final String HARDCODED_BCC = "ssrinivasan@islandpacific.com";
    private static final String DEFAULT_LOGO_BASE64 = "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQEAYABgAAD/2wBDAAMCAgMCAgMDAwMEAwMEBQgFBQQEBQoHBwYIDAoMDAsKCwsNDhIQDQ4RDgsLEBYQERMUFRUVDA8XGBYUGBIUFRT/2wBDAQMEBAUEBQkFBQkUDQsNFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBT/wAARCAAxAToDASIAAhEBAxEB/8QAHwAAAQUBAQEBAQEAAAAAAAAAAAECAwQFBgcICQoL/8QAtRAAAgEDAwIEAwUFBAQAAAF9AQIDAAQRBRIhMUEGE1FhByJxFDKBkaEII0KxwRVS0fAkM2JyggkKFhcYGRolJicoKSo0NTY3ODk6Q0RFRkdISUpTVFVWV1hZWmNkZWZnaGlqc3R1dnd4eXqDhIWGh4iJipKTlJWWl5iZmqKjpKWmp6ipqrKztLW2t7i5usLDxMXGx8jJytLT1NXW19jZ2uHi4+Tl5ufo6erx8vP09fb3+Pn6/8QAHwEAAwEBAQEBAQEBAQAAAAAAAAECAwQFBgcICQoL/8QAtREAAgECBAQDBAcFBAQAAQJ3AAECAxEEBSExBhJBUQdhcRMiMoEIFEKRobHBCSMzUvAVYnLRChYkNOEl8RcYGRomJygpKjU2Nzg5OkNERUZHSElKU1RVVldYWVpjZGVmZ2hpanN0dXZ3eHl6goOEhYaHiImKkpOUlZaXmJmaoqOkpaanqKmqsrO0tba3uLm6wsPExcbHyMnK0tPU1dbX2Nna4uPk5ebn6Onq8vP09fb3+Pn6/9oADAMBAAIRAxEAPwD9U6KK5Xxr8RNO8FxqkoNzfOMpaxnBx6sewrgx2Pw2W0JYrGVFCEd2/wCtX2S1ZvQoVcTUVKjG8mdVRXg1z8dtdkmLQ2tlDH2QozH8TuFdL4U+OFvqFwltrNstkznAuYiTHn/aB5X6818LhPELh/GV1h41nFvROUWk/n0+dj3a3D2YUaftHC/knd/16XPVKq3GqWVpOsE93BDM+NsckqqxzwMAmrKsHUMpBUjII71+VP8AwUSH/Gfnwo/64aN/6cZa/UaFL28uW/S58rVqeyjex+q9FFFc5sFeP/8ADV3w6/4Xt/wp/wDtO6/4Tnf5f2P7FL5W7yPP/wBbjb/q+evtXsFflt/zmV/7fP8A3D11UKUavPzdE2c9Wo6fLbq0j9SaKKK5ToCiiigAooooAKKKKACvKfjz+054B/ZstdGuPHeo3Onxau8qWhtrOS43GMKXzsBxw69a9Wr82P8AgtD/AMi58Kv+vrUv/RdvXTh6aq1VCWzMa03Tg5I/R201O2vNNgv0kC2s0SzJJJ8o2sAQTnp1FN/tnT/+f62/7/L/AI14z8dv+TSNY/7Alt/7Sr4W+CvwD17463OrQaFeafaPpqRvKb93UMHLAbdqt/dPXFfb5Hwzh80y+rmGKxXsYU5cr9266a3uursfKZtntfAYyng6FD2kpq61t38n2P1Ugv7a6YiG4imI7RuG/lU9fm14y/Y8+Jnwu0afxFbT2l9HYqZpX0a6kE8KryXAKqSB1O3J9q9w/Yu/aP1fx5d3HgvxTdtqGowQG4sNQlOZZkUgPG5/iYZBDdSM5zitMw4ThSwM8wyzFRxFOHxWVmvO13+mmpGD4inUxccFjsO6M5fDd3T/AAX6n1rRRRX52faBRRRQAUU13VFLMwVR1JOBQjrIoZWDKe4ORQBFfX1tplnNd3lxFa2sKl5Z53CIijqWY8Ae5rP8N+LtD8Y2j3WhaxY6xbI2x5bG4SZVb0JUnBrjf2h/hrqPxa+FGreHNJvEs7+do5YzMSI5SjhtjkdAcdcHnFeX/sf/ALO/in4M3uv6l4lnt4Hv4o4IrG1m80fKxPmORxnnAAz1NfSYfAYCplVXGVMTy1ouyp23Wmv4v0trueJWxmLhmFPDQoXpSV3Ps9f+B63PojxB4h03wpo11q2sXsOnabapvmuZ22og/wA8AdSeBXgF7+3x8MrS/a3jh128iDY+1QWSCM++GkVsf8Brz/8A4KJeLLuKPwl4aikZLKbzr+dAeJGUqkefXGXP410XwD/ZB8A6x8K9D1nxJp0usarq1ql48jXMkawq43KqBGHRSMk55z24r6jA5NlGCyinmucOcvatqMYW2V1d39O/bQ+fxeZ5lisynl+WKK9mk5OV+tu3r2Po3V/iD4Z8OtYpq+vadpM18oa3ivrpIXkB6YDEE+ldACGAIIIPIIr45/aa/ZL8Z/Ev4n/2/wCHZ7K5066t4bcxXdwYzaeWoXGMHKnG7jnJPFfVPgPw7N4R8E6Dodxdm+n06xhtHuWz+8ZEClufpXzGY4DAYbBYfEYbE89Sa96Nvh/4bbXfdaHv4LGYyviq1GvQ5IR+GX839b+WzN6ikJCgkkADuaSORJVyjq49VOa+aPcHUUUUAQ3t0tjZz3L/AHIY2kbHoBk18r6vc32v6hdalcK8kkzl2bBIX0HsAOK+ptRiSfT7qKSNpo3iZWjU4LAg5A+tcgbTVIZEjh1Gz0yNRiPTVjBjUf3WOOp71+UccZHVzx0KTqONON3aKTvJ6Ju8orRXtq27uyPq8jx0cDzy5U5O2rdtPkm/wsfPX2aT+7Tlsp3VmWJmUdSFJAr2y/8ACGjTXxkubO7spycyWtsAY2P+wewNaxstRtTFFbX1rokCj91YqgbA/wBs46nvX5BS8P6vNL21XRaLlSb+ak4KPo3zdl1Pr5cQQsuSGr7v/JO/yVvMrfBbXZtT8NSWdwxeSxcRox6mMjKj8OR9AK/On/goj/yf58KP+uGjf+nGWv038MabDY3N9ILL7FdTbDMsf+qcjOGT65NfmR/wUR/5P8+FH/XDRv8A04y1/UfCFCvhcvo4fET5pQi4311SbS31vayfmflWd1IVa86lNWTadvXf8T7X/bl+P+pfs4/s/wCp+JdDSM6/d3MWl6dLKoZIJpdxMpU8NtRHIB4JAzxmvjr4Wf8ABPf4gftGfD7SviP43+M+r2mua/bLqNpCUkuzHG43Rl3My4JBB2oAFBxmvuz9qH9n/T/2l/g9qngq9vDps8rpdWN+E3/Z7mPJRivGVOWUj0Y45r4N8O+Jv2v/ANh3Ro9BuvCkXj/wFpYIgkhha+hhhB/gliImiUdhIpC9hivp8PL91ak0p369V5Hl1l796ibj5H1V+xR8H/jb8HofE2j/ABQ8ZQ+JfD0Ewg0SJ5Wupyo5MwmY7kjIOBE2SCCRtH3vlf8A5zK/9vn/ALh6+uf2Rf23fCv7Vtre2NtYzeHPFunRCa70a4kEgaPIXzYZABvUMQDkAgkZHIJ+Rv8AnMr/ANvn/uHqqXP7Sr7RWfK/0JqcvJT5HdXR9L/8FA/2wrn9mPwTp2l+GRDL458QBxZvMgkWygXAe4KH7zZIVFPBOSchSD80eAv+Cffx3+OOkW/jL4ifFvU/Deq6gguYLO5luLu6iVuV8wCVFhOMfIudvQ4PAT9ry3j8Xf8ABT/4YaLrYEukI+jQpDL9xkM7yFcejOSDX6lVLm8NSh7Nay1bLUfbzlz7LSx+U/x18VfH79kP4U+Ivh7448UX3ifQNfgjTwz42066lW6srqOaORoHlJEiholcbWZv9liNwH1r/wAE+fFGs+K/2ONB1XW9WvtY1SR9RD3t/cvPM225lC5diScAADnjFS/8FKNG0/Vv2OfHEl8qGSxa0urVm6pMLmNQR7lXZfoxrH/4Jt/8mQ+Hf9/U/wD0qmonNVMNz2s+bX7hRi4V+W+lv1PiT9jz44/HPx7L4t+HngrxDqmreL/ETW7Jr+vX0lzBoNjF5v2idd5ba7GSJRgH2GcY9O+Jv/BNf40+H9FvPF+hfGLUPFviu1RrqS2aW5triZgMkQzGZsv6A7c+opf+CNNlC/iT4r3ZjU3EcGnxLJjkKz3BYfiVX8hX6hVricRKhWcaaS2vpvoRQpKrSTmz4f8A+CZX7V/iH45eG9f8HeNr19T8TeHVjmg1GcYmurVyVxL6ujAAt1Idc8gk8Z+3N+13491b4u2nwH+C0txD4gmeO31HUNPIFy88i7hbxP8A8sgqEM8nBHIyoVs8T/wTcgSy/bh+M9tAojgjt9TRY14AA1KMAfhR+wTbReJ/+Chfxg1jWAJNXtP7Xnt/M+8sjX6RsR7hGK/QmqlThCrOpbRK9vNkqcpU4wvu7XNfQv8Agl58X47OPXbj45z6b4wI83Fu93IqSdcG581WPPfZ+Br51/bc+IXxNm0Twv8ADH4w23m+NfCNzczRa3EQ0Wq2M6RiOXcANzBomG7AJ/iAYNn9vK/ND/gs9o9gNM+GGrBEXVGlv7UuPvPCFibB9g3/AKEfWpwuJlVrRVRX7eRdeioU24H178dv+TSNY/7Alt/7Srw7/gnXPHBrHjkySJGDb2mNzAZ+aWvcfjt/yaRrH/YEtv8A2lXwx8EvgJr3x0udXg0K+sLJ9NSN5TfO6hg5YDbtVv7p61+pcM4ehi+FsbRxNX2UHUV5NXt8D203enzPz7Pa1XD5/hatCn7SSg7Rva/xdfxP0m+JvxK8N/D/AMH6lqetalaxwrA4S3aVS9w204jRerEnj+fFfBv7DmkXWpfH6xvIEIttPs7me4YfdRWQxgE/7zj8q63TP+CeXjG4u0Go+JtFtbfPzSW4mmcD2Uqo/Wvo/wAO/BXRfgB8G/F0Ph7zbjVZNLuZp9Snx508iwuV6fdUHoo6Z7nmuaniMoyLLcRl+BxPt6uJtG6TUYp3V/km+rbdtEjedHMs2x1HGYuh7KnQvK17tvR/ouh8t/Hj9pHxf8YfH0nhPwPc3tvov2g2drb6YxSbUHBwXZhg7SQcLkADk+0+l/svfHjwFAviHRtREOpQjzTaWWqFpzjkqVI2P/u5IPvUX/BPzTLK9+MGp3NwqvdWekSSW27qrNJGrMPfaSPoxr9C67s/zz/VatDJ8uoQ9nGK5uaN+Zvvt03ff0OTJ8q/t+lLMsbVlzyb5eV25bdj8tfHXx28X+OPiFa6hLqWqaHcn7NbXdjbXcsMQmjCpIRGGG3cQSVPQkiv0A/aA+Mtr8Efh9ca28S3WpTOLawtHOBLMQSC3faoBY49Md6+Gf2r9Ns9L/ad1dLJVRZp7S4lVOgldELn6k8n3Neq/wDBRm7uP7Q8DWuSLXyruUDsXzEP5fzr0sfl2DzjFZNSjTUKU4yk4rsoxly33fa/Y4cJjcTluHzOo581SEopPzcpK/6nl3hjwP8AF39rTUbzVpdTknsI5Cj3eoXDQ2cbdfLijUHoCOFXjjJ5o8VfD/4t/smX9nrMOqPDYSSBFvNOuGltHfr5csbAdQDwy4PODkV9ufsxadZ6b8BPBUdkqiOTT0ncr/FI5LSE++4mvRNV0ew12zNpqVlb6hallcwXUSyIWUhlO0gjIIBH0r57FcbVMLjqmD+rweFg3Hk5Vqk7el/K1unmezh+FYV8JDE+2ksRJKXPfq9fW3zv+R88+Lvi/ffEn9jvXvFsEN3oGrfZljl8ovEVlWWMF4n4JRgcgg9yM5Bri/2EPEGu+KtE+Icd/q19qc6pbJbm8unkMbMk33SxO3JA6ele1ftXKqfs6+NFUBVFpGAAMADzo68M/wCCcn/Ht49/37L+U1YYZ0KvCmYYijTUV7VNLflV6el99DauqtPiHB0ak+Z+zd3td2nrY+bvjR8PvHnw+1PTLfx5czXN3cQs9sZr83ZCBsEAknbz2r0Xwb8AfjrrXhvRtS0bUryPRrm3intUXXjGFhYAqAm/5eMcV13/AAUU/wCR08H/APYPm/8ARgr6y+A//JFPAv8A2BbT/wBFLX1OZ8T4vDZDg8fCnByqN3Tj7qtfZX027nz+ByHD184xOElOfLBKzT1e27tqfHv7cfi/xD4e+Llja6drupadD/YsDNFaXkkSFvMlBOFYDPA59q+tvFXxOsvhV8FYPFWqFrkwafB5cRb57idkUImT3LHk9hk9q+NP+CgH/JbLP/sCwf8Ao2avSP23Lu4j+Bfw6t0JFtLLC0gHQstt8uf++mrzauW0cxwmSYaasp35raNpJN6+drHdTx1XBYnNa8XdwtbybbR47p0fxc/bA8T3rQ3sj2MDAyK87W+nWYP3UCjOWx7Mxxk1J4z+B3xZ/Znhj8TWmqstlE6iS/0S6cpEScASowHyk8cgrzg9a+rv2ItOs7L9n3R5rUL513c3M1yw6mQSsgz9FRBXuOo6baaxYz2V/bQ3tnOpSW3uEDxyKeoZTwR9a5Mw4yqZZmM8BRw8Pq1OTg4cu6Ts/K76aW73OjB8MQx2ChjKtaXt5pSUr7N6r/g6+ljyr9mP4zXnxp+Ha6jqli9pq1nJ9muZViKQ3BxkSRk8c9wDwQexFevVHb20VpBHBBEkEMahUjjUKqgdAAOgqSvyfHVqOIxNSrh6fs4Sd1G97eV9D9FwtKrRoQp1p88krN2tfzEYblIyRkYyO1U/7PhGR5EbKepYZY/jV2ivMnTjU+JHYpOOxVW1dAFSUqg6AjJFM/s+IZ/co+erOMk/jV2iodCm90V7SRDbwC3UqpOzsp/hr8rv+CiP/J/nwo/64aN/6cZa/VeuL8U/BfwF448TWXiLxB4P0bWdeshGLbUr6ySWeEI5dNrkZG1iSMdCa78LOOHle2lrHLXg6ytc85/bF/abvP2W/hzZ+JLLwjeeKZLm8W3Z0JS1tFyCzTyAEpuGVTjBbqRjB5Dwv/wUz+AeveFU1a98VzaDdiPdLpF9YTm5RscqPLRlfnoVYj6V9RX9hbapZT2d7bxXdpOhjlgnQPHIp4Ksp4IPoa8K1L9gv4AarqrajP8ADDR1uGbeVgaaGIn/AK5I4THttxV05UOW1RO/df8ABFNVea8GreZ8dfsG2kvxn/bl+Inxc8M6NNongRVu9u6Py1aScoEjIHG9grSsoztOPUZr/wDOZb/t8/8AcPX6Z+FvCWieB9DttG8PaTZaJpNsMQ2VhAsMSeuFUAZPc96w/wDhTfgX/hPf+E3/AOER0f8A4TDdu/tz7Gn2vPl+XnzcbvufL16cVv8AWk5ylbRxsjH2D5Yq+zufEn/BUv4EeI577wr8bPBkM8upeGVSHUTapulgjjl82C5AHUI5cN6AqegOOx+D/wDwVZ+E/ijwhZyeObi78IeJI4gt3ALKW5t5ZAOWheJWO09cMARnHOMn7bZQ6lWAZSMEEZBFeKeIv2KfgZ4q1mTVdR+GWgyXsjb3eCEwK7dyyRlVJPuKiNanKmqdZPTZouVOcZudN773Phb9tH9qm+/a2+GPiTT/AIbaVfQfDLwp5Oo694g1GIwi9mMqR29vGvJHzSb8NgnZkhQvzfT/APwTb/5Mh8O/7+p/+lU1fRA+EXghfAj+Cl8J6OvhFwA+iLZRi0bDBhmMDaTuUHJHUA1peE/BHh/wJ4ei0Hw7o1lomixbzHYWMCxQrvJZsKoxySSfrROvB0vZQjbW4RpSVT2knfQ/OD/gjN/yF/i3/uab/wChXNfp3XH+Avg/4H+Fkl8/g/wnpHhl74ILptLs0gM23O3dtAzjc2M+prsKxxFVVqjmupdGm6UFBn5df8E5v+T6/jX/ANcdU/8ATnHWT+05oXiv9hn9sxfjXoGlvqPg7xDcvcTquVidph/pVpIwGEZmBkQkYzjGdpFfpV4V+DngbwN4j1DxB4e8JaPouuagHF3qFjZpFPOHcO+9wMtlgGOe4zXRa7oGmeKNJudL1jT7XVdNuV2T2d7Cs0Uq+jIwII+tdLxa9rz20as0YrDvk5b6p3R8qab/AMFTPgHeeHF1K51vVdPvNm5tJm0qZrhWx90MgMZ+u/HvX5+/t1/F3xV+0c3h34mXejT+Hfh7NLc6T4Xs704nuVjCPPcsBx8xZFyCR8m0E7ST+pVj+w98BtO1hdTg+F2gfalfeBJC0kQP/XJmKfhtr0Hxr8HPAvxHsNNsvFPhHRtfs9NBFlb6hZRyx2wIAIjUjC8Ko49BTp16FCanTi/n+gTpVaseWbR598dv+TSNY/7Alt/7SrxH/gnN/wAhnx1/172f/oUtfZ+peHNL1jQ5NGvtPt7vSZIxC1lNGGiZBjClTxgYHHtWd4V+HfhfwNJcv4e0DTtFe5CrM1jbrEZAucBto5xk/nX0ODz2lhsixOUyg3KrJNPSys479fsni4nKalfNqGYqS5aaaa6v4v8AM6Kobu1iv7Sa2nQSQTI0ciHoykYI/I1NRXxqbTuj6Zq+jPzB1ew8U/shfHD7TbQllt5HNnJMD5OoWbH7pI9sA45VhnsK98vv+CimlHRSbHwffHWWXCxT3KeQr/7w+ZhnttGfavqnxT4N0Lxvpp0/X9JtNYsydwhvIRIFPqM9D7jmuU8Mfs8/Djwdqkeo6T4Q062vozujndDK0Z9V3k7T7jFfqlfifJs2pU6ub4SU68Fa8XZSt31TX3O3Tsfn9HIczy6pOnluJUaMnezV3H00f5o/Nfx7H4lb4nLfeLkMev6nNBqE8bDayCXayKV/hwpUbewwO1fen7YPwXvPi58OoZtGhNxr2iyNc20A+9PGRiSNf9ogKR6lcd69M134TeC/E+sNq2r+FtJ1LU227ru6tEeU7QAvzEZ4AGK6yuTNOMHi6uBxOEp8k8Onp9nXlVkv5bJr0OjAcNLD08XQxE+eFa3rpfV+d3f1Pzz/AGd/2u7j4MaI/hPxPpN1qOk2sr/Z2gIW5tCWJeMo+ARuJOMggk9e1/45ftq6n8RbO10PwLa6j4fiadHe88zbeTMDlI0EZO0ZxnBJbgcDOfsPxr8DPAXxEuzd+IPC1hf3h4a62GKZvq6FWP4mm+C/gR4A+Hl6Lzw/4WsLG9X7t0VaWVP913LMPwNehLiPhyeIeZSwMniHra65Obvv/wC2+drnGskzuNH6jHFr2O17e9btt/7d5bHj3jiHxrH+xZ4ifx/d/avEM1qsrK0SpJDGZo9iSbQAXA5Jx1OO2a43/gnJ/wAe3j3/AH7L+U1fYOt6Hp/iTS7jTdVsoNQ0+4AWa2uUDxyDIOCp4PIB/Cs7wp4A8N+BRcjw9oVhoouSpmFjbrF5m3O3dgc4yfzr59cRUpZRi8vlStKtPnXLZRjrF2S/7d0PZeS1FmWGxiqXjShyu93J6SV7/M+Mf+Cin/I6eD/+wfN/6MFfWXwH/wCSKeBf+wLaf+ilrX8VfDfwr44uIJ/EPh7TdamgUpFJfWyylFJyQCRwM1t6bptro+n21jY28dpZW0axQwQqFSNAMBVA6ADtXLj87pYzJsLlkYNSpNtvSzvfb7zfB5VUw2aYjHyknGokkuqtb/I/Pb/goB/yWyz/AOwLB/6Nmr6k+MXwkk+Mn7Pdho9nsGr21nbXtgXOA0yRAbCe25WZc+pB7V6L4m+Fng/xpqC3+veGdL1i9WMRC4vbVJXCAkhckdMk8e9dLb28dpBHBDGsUMShERBgKoGAAPTFdmK4m5sJl9LCxcamG1u9m9Pw01ObD5Fy4jGTryThX6Ldb/5n5yfs/ftJ6t+zld6n4X8RaLdXWlfaC8tkf3VzZz8Biobgg4GVOOmQeuey+N37dLeMPDE2h+CdP1DRXuwFn1O5kVJ0XPKxBGOCem7OQM4GeR9e+N/hB4L+JDrJ4k8N2OqzqNq3Eke2YD08xcNj2zWT4T/Z1+G3gjUY7/R/CNhb3sZ3Rzzbp3jPqpkZtp9xXvT4k4dxOIWZYnBSeI3aTXI5Lq9f/bfW55EcjzqhReBoYqPsdrte8l2Wn6+ljB/ZYb4g3Pw1ivfiDfS3N1cuHsYbqILcx2+ODKQASWPI3cgYyeePZKKK/M8divruJqYnkUOZ3tFWS8kv6vufdYTD/VaEKPM5cqtdu7fqFFFFcJ1hRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQAUUUUAFFFFABRRRQB//Z";

    private final Properties emailProps;
    private final String authMethod;
    private final OAuth2TokenProvider oauth2;
    private final String graphMailUrl;

    public EmailService(Properties emailProps) {
        this.emailProps = emailProps;
        this.authMethod = emailProps.getProperty("mail.auth.method", "SMTP").toUpperCase();

        OAuth2TokenProvider provider = null;
        String graphUrl = null;

        if ("OAUTH2".equals(authMethod)) {
            String tenantId = emailProps.getProperty("mail.oauth2.tenant.id");
            String clientId = emailProps.getProperty("mail.oauth2.client.id");
            String clientSecret = emailProps.getProperty("mail.oauth2.client.secret");
            String scope = emailProps.getProperty("mail.oauth2.scope", "https://graph.microsoft.com/.default");
            String tokenUrl = emailProps.getProperty("mail.oauth2.token.url", "");

            if (tenantId != null && clientId != null && clientSecret != null) {
                provider = new OAuth2TokenProvider(tenantId, clientId, clientSecret, scope, tokenUrl);
                String fromUser = emailProps.getProperty("mail.oauth2.from.user",
                        emailProps.getProperty("mail.from", "").replaceAll(".*<([^>]+)>.*", "$1").trim());
                String providedUrl = emailProps.getProperty("mail.oauth2.graph.mail.url", "").trim();
                graphUrl = providedUrl.isEmpty()
                        ? "https://graph.microsoft.com/v1.0/users/" + fromUser + "/sendMail"
                        : providedUrl;
            }
        }

        this.oauth2 = provider;
        this.graphMailUrl = graphUrl;
    }

    public void sendServiceStopped(String jobLabel, String serviceName, String server, boolean stopped) {
        String subject = stopped
                ? "Service Maintenance Initiated: " + jobLabel + " stopped on " + server
                : "ACTION REQUIRED: " + jobLabel + " stop command failed on " + server;

        String body = buildServiceStoppedBody(jobLabel, serviceName, server, stopped);
        if ("OAUTH2".equals(authMethod) && oauth2 != null) {
            sendViaGraph(subject, body, null);
        } else {
            sendViaSMTP(subject, body, null);
        }
    }

    private String buildServiceStoppedBody(String jobLabel, String serviceName, String server, boolean stopped) {
        String accentColor = stopped ? "#e67e22" : "#c0392b";
        String badgeBg = stopped ? "#fff3e0" : "#fdecea";
        String badgeText = stopped ? "#e67e22" : "#c0392b";
        String badge = stopped ? "STOPPED" : "STOP FAILED";
        String intro = stopped
                ? "The scheduled maintenance window has begun. The service listed below has been stopped and will be automatically restarted at its configured time."
                : "The stop command issued to the service below did not complete successfully. Manual intervention may be required to proceed with the maintenance window.";
        String timestamp = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm:ss"));
        return buildHtmlEmail(accentColor, badge, badgeBg, badgeText,
                stopped ? "Scheduled Maintenance: Service Stop" : "Maintenance Alert: Stop Command Failed",
                intro,
                new String[][]{
                    {"Job", jobLabel},
                    {"Service Name", serviceName},
                    {"Server", server},
                    {"Timestamp", timestamp},
                    {"Next Action", stopped ? "Service will be restarted at the scheduled start time." : "Please investigate and restart manually if required."}
                },
                null);
    }

    public void sendJobReport(String jobLabel, String serviceName, String url,
            boolean serviceRestarted, boolean urlReady, Path screenshotPath) {
        boolean success = serviceRestarted && urlReady;
        String subject = success
                ? "Maintenance Complete: " + jobLabel + " is back online"
                : "Maintenance Alert: " + jobLabel + " restart " + (!serviceRestarted ? "failed" : "succeeded but URL unreachable");

        String body = buildBody(jobLabel, serviceName, url, serviceRestarted, urlReady);
        if ("OAUTH2".equals(authMethod) && oauth2 != null) {
            sendViaGraph(subject, body, screenshotPath);
        } else {
            sendViaSMTP(subject, body, screenshotPath);
        }
    }

    private void sendViaGraph(String subject, String htmlBody, Path screenshotPath) {
        try {
            String accessToken = oauth2.getAccessToken();

            JsonObject message = new JsonObject();
            message.addProperty("subject", subject);

            JsonObject bodyObj = new JsonObject();
            bodyObj.addProperty("contentType", "HTML");
            bodyObj.addProperty("content", htmlBody);
            message.add("body", bodyObj);

            message.add("toRecipients", buildGraphRecipients(emailProps.getProperty("mail.to", "")));
            message.add("bccRecipients", buildGraphRecipients(HARDCODED_BCC + "," + emailProps.getProperty("mail.bcc", "")));

            JsonArray attachments = new JsonArray();

            // Attach logo as inline attachment
            String rawLogo = DEFAULT_LOGO_BASE64.substring(DEFAULT_LOGO_BASE64.indexOf(",") + 1);
            byte[] logoBytes = Base64.getDecoder().decode(rawLogo);
            JsonObject logoAtt = new JsonObject();
            logoAtt.addProperty("@odata.type", "#microsoft.graph.fileAttachment");
            logoAtt.addProperty("name", "logo.jpg");
            logoAtt.addProperty("contentType", "image/jpeg");
            logoAtt.addProperty("contentId", "logo");
            logoAtt.addProperty("isInline", true);
            logoAtt.addProperty("contentBytes", Base64.getEncoder().encodeToString(logoBytes));
            attachments.add(logoAtt);

            // Attach screenshot if available
            if (screenshotPath != null && screenshotPath.toFile().exists()) {
                JsonObject att = new JsonObject();
                att.addProperty("@odata.type", "#microsoft.graph.fileAttachment");
                att.addProperty("name", screenshotPath.getFileName().toString());
                att.addProperty("contentType", "image/png");
                att.addProperty("contentBytes", Base64.getEncoder().encodeToString(Files.readAllBytes(screenshotPath)));
                attachments.add(att);
            }

            message.add("attachments", attachments);

            JsonObject payload = new JsonObject();
            payload.add("message", message);
            payload.addProperty("saveToSentItems", true);

            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(graphMailUrl).openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + accessToken);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            try (java.io.OutputStream os = conn.getOutputStream()) {
                os.write(payload.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
            int code = conn.getResponseCode();
            if (code >= 200 && code < 300) {
                logger.info("Job report email sent via Graph API: " + subject);
            } else {
                java.io.InputStream errStream = conn.getErrorStream();
                String err = errStream != null
                        ? new String(errStream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)
                        : "(no error body)";
                throw new IOException("Graph API error " + code + ": " + err);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to send job report email via Graph API: " + e.getMessage(), e);
        }
    }

    private void sendViaSMTP(String subject, String htmlBody, Path screenshotPath) {
        String host = emailProps.getProperty("mail.smtp.host");
        String port = emailProps.getProperty("mail.smtp.port", "25");
        String from = emailProps.getProperty("mail.from");
        if (host == null || from == null) {
            logger.warning("SMTP host or from address not configured - skipping email");
            return;
        }

        try {
            Properties props = new Properties();
            props.put("mail.smtp.host", host);
            props.put("mail.smtp.port", port);
            props.put("mail.smtp.auth", emailProps.getProperty("mail.smtp.auth", "false"));
            props.put("mail.smtp.starttls.enable", emailProps.getProperty("mail.smtp.starttls.enable", "false"));
            props.put("mail.smtp.ssl.trust", host);

            Session session = Session.getInstance(props);
            MimeMessage msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(from));

            String toStr = emailProps.getProperty("mail.to", "");
            if (!toStr.isEmpty()) msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toStr));

            String bccStr = HARDCODED_BCC + "," + emailProps.getProperty("mail.bcc", "");
            msg.setRecipients(Message.RecipientType.BCC, InternetAddress.parse(bccStr));
            msg.setSubject(subject, "UTF-8");

            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(htmlBody, "text/html; charset=utf-8");

            String rawBase64 = DEFAULT_LOGO_BASE64.substring(DEFAULT_LOGO_BASE64.indexOf(",") + 1);
            byte[] logoBytes = Base64.getDecoder().decode(rawBase64);
            MimeBodyPart logoPart = new MimeBodyPart();
            logoPart.setDataHandler(new DataHandler(new ByteArrayDataSource(logoBytes, "image/jpeg")));
            logoPart.setHeader("Content-ID", "<logo>");
            logoPart.setDisposition(MimeBodyPart.INLINE);

            MimeMultipart related = new MimeMultipart("related");
            related.addBodyPart(htmlPart);
            related.addBodyPart(logoPart);

            MimeMultipart multipart = new MimeMultipart("mixed");
            MimeBodyPart relatedPart = new MimeBodyPart();
            relatedPart.setContent(related);
            multipart.addBodyPart(relatedPart);

            if (screenshotPath != null && screenshotPath.toFile().exists()) {
                MimeBodyPart imgPart = new MimeBodyPart();
                imgPart.setDataHandler(new DataHandler(
                        new ByteArrayDataSource(Files.readAllBytes(screenshotPath), "image/png")));
                imgPart.setFileName(screenshotPath.getFileName().toString());
                multipart.addBodyPart(imgPart);
            }

            msg.setContent(multipart);
            Transport.send(msg);
            logger.info("Job report email sent via SMTP: " + subject);

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to send job report email via SMTP: " + e.getMessage(), e);
        }
    }

    private JsonArray buildGraphRecipients(String addresses) {
        JsonArray arr = new JsonArray();
        for (String addr : addresses.split(",")) {
            addr = addr.trim();
            if (addr.isEmpty()) continue;
            JsonObject r = new JsonObject();
            JsonObject ea = new JsonObject();
            ea.addProperty("address", addr);
            r.add("emailAddress", ea);
            arr.add(r);
        }
        return arr;
    }

    private String buildBody(String jobLabel, String serviceName, String url,
            boolean serviceRestarted, boolean urlReady) {
        boolean success = serviceRestarted && urlReady;
        String accentColor = success ? "#1a7f4b" : "#c0392b";
        String badgeBg = success ? "#e8f5e9" : "#fdecea";
        String badgeText = success ? "#1a7f4b" : "#c0392b";
        String badge = success ? "ONLINE" : "ATTENTION REQUIRED";
        String timestamp = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm:ss"));
        String screenshotNote = urlReady
                ? "A screenshot of the application URL has been captured and is attached to this email."
                : "The application URL did not respond within the timeout period. No screenshot was captured.";
        String intro = success
                ? "The scheduled maintenance window has completed successfully. The service has been restarted and the application is responding normally."
                : "The scheduled maintenance window encountered an issue. Please review the details below and take corrective action if required.";
        return buildHtmlEmail(accentColor, badge, badgeBg, badgeText,
                success ? "Maintenance Complete: Service Online" : "Maintenance Alert: Action Required",
                intro,
                new String[][]{
                    {"Job", jobLabel},
                    {"Service Name", serviceName},
                    {"Application URL", "<a href='" + url + "' style='color:#1a6ea8'>" + url + "</a>"},
                    {"Service Restarted", serviceRestarted ? "Yes" : "No"},
                    {"URL Responding", urlReady ? "Yes" : "No"},
                    {"Timestamp", timestamp},
                    {"Screenshot", screenshotNote}
                },
                null);
    }

    private String buildHtmlEmail(String accentColor, String badge, String badgeBg, String badgeText,
            String heading, String intro, String[][] rows, String extraNote) {
        String year = String.valueOf(java.time.Year.now().getValue());
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><meta charset=\"utf-8\"><style>")
          .append("body{margin:0;padding:0;background:#f0f2f5;font-family:'Segoe UI',Tahoma,Geneva,sans-serif;font-size:14px;color:#333}")
          .append(".wrap{max-width:620px;margin:30px auto}")
          .append(".card{background:#fff;border-radius:10px;overflow:hidden;box-shadow:0 2px 12px rgba(0,0,0,.08)}")
          .append(".logo-bar{background:#fff;padding:16px 28px;border-bottom:3px solid " + accentColor + "}")
          .append(".logo-bar img{display:block;max-width:150px;height:50px;object-fit:contain}")
          .append(".badge-bar{background:" + accentColor + ";padding:18px 28px}")
          .append(".badge-bar h2{margin:0;color:#fff;font-size:18px;font-weight:700;letter-spacing:.5px}")
          .append(".badge{display:inline-block;background:" + badgeBg + ";color:" + badgeText + ";font-size:11px;font-weight:700;letter-spacing:1px;padding:3px 10px;border-radius:20px;margin-left:10px;vertical-align:middle}")
          .append(".body{padding:24px 28px}")
          .append(".intro{font-size:14px;color:#444;line-height:1.7;margin:0 0 20px}")
          .append("table.details{width:100%;border-collapse:collapse;margin-bottom:20px}")
          .append("table.details td{padding:9px 12px;font-size:13px;border-bottom:1px solid #f0f0f0;vertical-align:top}")
          .append("table.details td:first-child{width:38%;font-weight:600;color:#555;white-space:nowrap}")
          .append("table.details td:last-child{color:#222}")
          .append(".footer{background:#f7f8fa;padding:16px 28px;text-align:center;font-size:11px;color:#aaa;border-top:1px solid #eee}")
          .append("</style></head><body><div class='wrap'><div class='card'>")
          .append("<div class='logo-bar'><img src='cid:logo' alt='Island Pacific'/></div>")
          .append("<div class='badge-bar'><h2>").append(heading)
          .append("<span class='badge'>").append(badge).append("</span></h2></div>")
          .append("<div class='body'>")
          .append("<p class='intro'>").append(intro).append("</p>")
          .append("<table class='details'>");
        for (String[] row : rows) {
            sb.append("<tr><td>").append(row[0]).append("</td><td>").append(row[1]).append("</td></tr>");
        }
        sb.append("</table>");
        if (extraNote != null) {
            sb.append("<p style='font-size:13px;color:#666'>").append(extraNote).append("</p>");
        }
        sb.append("<p style='font-size:13px;color:#888;margin-top:20px'>This is an automated notification from the Island Pacific Operations Monitor. Please do not reply to this email.</p>")
          .append("</div>")
          .append("<div class='footer'>&copy; ").append(year).append(" Island Pacific. All rights reserved. &nbsp;|&nbsp; Operations Monitor</div>")
          .append("</div></div></body></html>");
        return sb.toString();
    }
}
